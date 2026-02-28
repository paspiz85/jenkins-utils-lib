
private Map _parseRepoURL(String urlIn) {
  def url = urlIn.trim().replaceAll(/\.git$/, '')
  def prefixSsh   = 'git@github.com:'
  def prefixHttps = 'https://github.com/'
  def prefixHttp  = 'http://github.com/'
  String path
  if (url.startsWith(prefixSsh)) {
    path = url.substring(prefixSsh.length())
  } else if (url.startsWith(prefixHttps)) {
    path = url.substring(prefixHttps.length())
  } else if (url.startsWith(prefixHttp)) {
    path = url.substring(prefixHttp.length())
  } else {
    error "[github] URL non riconosciuto: ${urlIn}"
  }
  def parts = path.split('/')
  if (parts.size() < 2) {
    error "[github] URL non valido (owner/repo mancante): ${urlIn}"
  }
  return [owner: parts[0], repo: parts[1]]
}

def release(Map args = [:]) {
  def credentialsId = args.credentialsId ?: args.credentialId ?: env.GITHUB_CREDS_ID
  if (!credentialsId) {
    error "[github] 'credentialsId' not provided and GITHUB_CREDS_ID not set"
  }
  def tag = args.tag?.toString()?.trim()
  if (!tag) {
    error "[github] tag is required"
  }
  def repoUrl = (args.url ?: env.GIT_URL)?.toString()?.trim()
  if (!repoUrl) {
    error "[github] url not provided and GIT_URL not set"
  }
  def repoInfo = _parseRepoURL(repoUrl)
  def owner = repoInfo.owner
  def repo  = repoInfo.repo
  def releaseId = null
  withCredentials([
    usernamePassword(credentialsId: credentialsId, usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_TOKEN')
  ]) {
    echo "Creazione release GitHub per ${owner}/${repo} - Tag: ${tag}"
    def requestBody = groovy.json.JsonOutput.toJson([
      tag_name               : tag,
      name                   : args.name ?: tag,
      body                   : args.body ?: tag,
      draft                  : (args.draft ?: false),
      prerelease             : (args.prerelease ?: false),
      generate_release_notes : (args.generate_release_notes ?: false)
    ])
    withEnv([
      "GITHUB_OWNER=${owner}",
      "GITHUB_REPO=${repo}",
      "GITHUB_REQUEST=${requestBody}"
    ]) {
      def out = sh(
        script: '''
          set -e
          # Print body to stdout and append status code as last line
          printf "%s" "$GITHUB_REQUEST" | \
            curl -sS \
              -H "Authorization: Bearer ${GITHUB_TOKEN}" \
              -H "Accept: application/vnd.github+json" \
              -H "Content-Type: application/json" \
              -X POST "https://api.github.com/repos/${GITHUB_OWNER}/${GITHUB_REPO}/releases" \
              --data-binary @- \
              -w "\\n%{http_code}"
        ''',
        returnStdout: true
      ).trim()
      def lines = out.readLines()
      if (lines.isEmpty()) error("[github] Empty response from GitHub")
      def responseStatus = lines.last().trim().toInteger()
      def responseBody = (lines.size() > 1) ? lines[0..-2].join('\n') : ""
      if (responseStatus == 201) {
        def responseJSON = new groovy.json.JsonSlurper().parseText(responseBody)
        echo "[github] release created"
        releaseId = responseJSON.id
        def releaseURL = responseJSON?.html_url ?: '(no url received)'
        echo "[github] ${releaseURL}"
      } else if (responseStatus == 422 && (args.skip_if_exists ?: false)) {
        echo "[github] release exists for tag ${tag}, skipped"
      } else {
        echo "[github] release failure with HTTP error ${responseStatus}:"
        echo responseBody
        error "[github] release failure"
      }
    }
    if (releaseId != null) {
      def assets = (args.assets ?: [])
      assets.each { asset ->
        def assetFilePath = asset.file?.toString()
        if (!assetFilePath) error "[github] asset.file is required"
        if (!fileExists(assetFilePath)) {
          error "[github] Asset file not found: ${assetFilePath}"
        }
        def assetName = asset.name?.toString()
        if (!assetName) {
          assetName = assetFilePath.tokenize('/\\').last()
        }
        def assetNameEnc = java.net.URLEncoder.encode(assetName, 'UTF-8')
        def contentType = (asset.contentType ?: 'application/octet-stream').toString()
        withEnv([
          "GITHUB_OWNER=${owner}",
          "GITHUB_REPO=${repo}",
          "GITHUB_RELEASE_ID=${releaseId}",
          "GITHUB_ASSET_FILE=${assetFilePath}",
          "GITHUB_ASSET_NAME=${assetNameEnc}",
          "GITHUB_ASSET_CONTENT_TYPE=${contentType}"
        ]) {
          def out = sh(
            script: '''
              set -e
              # Body + status code
              curl -sS \
                -H "Authorization: Bearer ${GITHUB_TOKEN}" \
                -H "Accept: application/vnd.github+json" \
                -H "Content-Type: ${GITHUB_ASSET_CONTENT_TYPE}" \
                -X POST "https://uploads.github.com/repos/${GITHUB_OWNER}/${GITHUB_REPO}/releases/${GITHUB_RELEASE_ID}/assets?name=${GITHUB_ASSET_NAME}" \
                --data-binary @"$GITHUB_ASSET_FILE" \
                -w "\\n%{http_code}"
            ''',
            returnStdout: true
          ).trim()
          def lines = out.readLines()
          if (lines.isEmpty()) error("[github] Empty response from GitHub")
          def responseStatus = lines.last().trim().toInteger()
          def responseBody = (lines.size() > 1) ? lines[0..-2].join('\n') : ""
          if (responseStatus == 201) {
            def responseJSON = new groovy.json.JsonSlurper().parseText(responseBody)
            echo "[github] uploaded asset ${responseJSON.name}"
            def assetURL = responseJSON?.browser_download_url ?: '(no url received)'
            echo "[github] ${assetURL}"
          } else if (responseStatus == 422) {
            echo "[github] asset exists ${assetName}, skipped"
          } else {
            echo "[github] upload asset failure with HTTP error ${responseStatus}:"
            echo responseBody
            error "[github] upload asset failure"
          }
        }
      }
    }
    return [
      owner   : owner,
      repo    : repo,
      tag     : tag,
      release : releaseId,
      url     : "https://github.com/${owner}/${repo}/releases/tag/${tag}"
    ]
  }
}

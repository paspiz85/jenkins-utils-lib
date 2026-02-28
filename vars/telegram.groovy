
def call(Map args = [:]) {
    sendMessage(args)
}

def sendMessage(Map args = [:]) {
  if (!args.message) {
    error "[telegram] Parameter 'message' is required"
  }
  def parseMode = args.parseMode ?: env.TELEGRAM_PARSE_MODE
  def chatId = args.chatId ?: env.TELEGRAM_CHAT_ID
  if (!chatId) {
    error "[telegram] Parameter 'chatId' not provided and TELEGRAM_BOT_CHAT_ID not set"
  }
  def threadId = args.threadId ?: env.TELEGRAM_THREAD_ID
  def credentialId = args.credentialId ?: env.TELEGRAM_BOT_TOKEN_ID
  if (!credentialId) {
    error "[telegram] Parameter 'credentialId' not provided and TELEGRAM_BOT_TOKEN_ID not set"
  }
  withCredentials([
    string(credentialsId: credentialId, variable: 'TELEGRAM_BOT_TOKEN')
  ]) {
    withEnv([
      "TELEGRAM_CHAT_ID=${chatId}",
      "TELEGRAM_THREAD_ID=${threadId ?: ''}",
      "TELEGRAM_MESSAGE=${args.message}",
      "TELEGRAM_PARSE_MODE=${parseMode ?: ''}"
    ]) {
      sh '''
        set -e
        curl -sS -f -X POST "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage" \
          -d "chat_id=${TELEGRAM_CHAT_ID}" \
          ${TELEGRAM_THREAD_ID:+-d "message_thread_id=${TELEGRAM_THREAD_ID}"} \
          ${TELEGRAM_PARSE_MODE:+-d "parse_mode=${TELEGRAM_PARSE_MODE}"} \
          --data-urlencode "text=${TELEGRAM_MESSAGE}"
      '''
    }
  }
}

def notifyDeploySuccess(Map args = [:]) {
  def msg = """🚀 Deploy SUCCESS
Job: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
URL: ${env.BUILD_URL}"""
  sendMessage(args + [message: msg])
}

def notifyBuildFailure(Map args = [:]) {
  def msg = """❌ Build FAILED
Job: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
URL: ${env.BUILD_URL}"""
  sendMessage(args + [message: msg])
}

def notifyBuildFixed(Map args = [:]) {
  def msg = """✅ Build FIXED
Job: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
URL: ${env.BUILD_URL}"""
  sendMessage(args + [message: msg])
}

def notifyBuildSuccess(Map args = [:]) {
  def msg = """✅ Build SUCCESS
Job: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
URL: ${env.BUILD_URL}"""
  sendMessage(args + [message: msg])
}

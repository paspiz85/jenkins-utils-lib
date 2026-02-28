
def call(Map a = [:]) {
    sendMessage(a)
}

def sendMessage(Map a = [:]) {
  if (!a.message) {
    error "[telegram] Parameter 'message' is required"
  }
  def parseMode = a.parseMode ?: env.TELEGRAM_BOT_PARSE_MODE
  def chatId = a.chatId ?: env.TELEGRAM_BOT_CHAT_ID
  if (!chatId) {
    error "[telegram] Parameter 'chatId' not provided and TELEGRAM_BOT_CHAT_ID not set"
  }
  def threadId = a.threadId ?: env.TELEGRAM_BOT_THREAD_ID
  def credentialId = a.credentialId ?: env.TELEGRAM_BOT_TOKEN_ID
  if (!credentialId) {
    error "[telegram] Parameter 'credentialId' not provided and TELEGRAM_BOT_TOKEN_ID not set"
  }
  withCredentials([
    string(credentialsId: credentialId, variable: 'TELEGRAM_BOT_TOKEN')
  ]) {
    withEnv([
      "TELEGRAM_CHAT_ID=${chatId}",
      "TELEGRAM_THREAD_ID=${threadId ?: ''}",
      "TELEGRAM_MESSAGE=${a.message}",
      "TELEGRAM_PARSE_MODE=${parseMode ?: ''}"
    ]) {
      sh '''
        set -e
        BASE_URL="https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage"
        # Use an array to avoid word-splitting issues with spaces/special chars
        ARGS=( -d "chat_id=${TELEGRAM_CHAT_ID}" --data-urlencode "text=${TELEGRAM_MESSAGE}" )
        if [ -n "${TELEGRAM_THREAD_ID}" ]; then
          ARGS+=( -d "message_thread_id=${TELEGRAM_THREAD_ID}" )
        fi
        if [ -n "${TELEGRAM_PARSE_MODE}" ]; then
          ARGS+=( -d "parse_mode=${TELEGRAM_PARSE_MODE}" )
        fi
        curl -sS -f -X POST "$BASE_URL" "${ARGS[@]}"
      '''
    }
  }
}

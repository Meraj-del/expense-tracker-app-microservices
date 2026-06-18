import logging
from app.utils.messageUtil import MessageUtil
from app.service.llmService import LLMService

logger = logging.getLogger(__name__)

class MessageService:
    def __init__(self):
        logger.info("Initializing MessageService")
        self.messageUtil = MessageUtil()
        self.llmService = None

    def process_message(self, message):
        logger.info("Checking if bank SMS")

        if self.messageUtil.isBankSms(message):
            logger.info("Bank SMS detected")
            try:
                result = self.llmService.runLLM(message)
                logger.info("LLM Result: %s", result)
                return result
            except Exception as e:
                logger.error("LLM Error: %s", str(e))
                return None

        logger.info("Not a bank SMS - skipping")
        return None
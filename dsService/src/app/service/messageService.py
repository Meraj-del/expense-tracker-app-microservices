from app.utils.messageUtil import MessageUtil
from app.service.llmService import LLMService

class MessageService:
    def __init__(self):
        print("Initializing MessageService")
        self.messageUtil = MessageUtil()
        self.llmService = LLMService()

    def process_message(self, message):
        print("Checking if bank SMS")
        
        if self.messageUtil.isBankSms(message):
            print("Bank SMS detected")

            try:
                result = self.llmService.runLLM(message)
                print("LLM Result:", result)
                return result

            except Exception as e:
                print("LLM Error:", str(e))
                return None

        print("Not bank SMS")
        return None
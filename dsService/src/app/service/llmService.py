from langchain_core.prompts import ChatPromptTemplate
from langchain_mistralai import ChatMistralAI
from dotenv import load_dotenv
from app.service.Expense import Expense
import logging
import os

logger = logging.getLogger(__name__)

class LLMService:
    def __init__(self):
        logger.info("Initializing LLM Service")
        load_dotenv(dotenv_path="/app/.env")
        self._runnable = None

    def _get_runnable(self):
        if self._runnable is None:
            api_key = os.getenv("MISTRAL_API_KEY")
            if not api_key:
                raise ValueError("MISTRAL_API_KEY not found in .env")

            llm = ChatMistralAI(
                api_key=api_key,
                model="mistral-large-latest"
            )

            prompt = ChatPromptTemplate.from_messages([
                ("system", "Extract expense details. Return null if unknown."),
                ("human", "{text}")
            ])

            self._runnable = prompt | llm.with_structured_output(Expense)
            logger.info("LLM runnable initialized successfully")

        return self._runnable

    def runLLM(self, message):
        logger.info("Calling LLM for message processing")
        result = self._get_runnable().invoke({"text": message})
        logger.info("LLM response received: %s", result)
        return result
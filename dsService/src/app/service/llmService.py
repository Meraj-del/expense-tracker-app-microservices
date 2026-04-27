from langchain_core.prompts import ChatPromptTemplate
from langchain_mistralai import ChatMistralAI
from dotenv import load_dotenv
import os
from app.service.Expense import Expense

class LLMService:
    def __init__(self):
        print("Initializing LLM Service")

        load_dotenv(dotenv_path="/app/.env")

        self.prompt = ChatPromptTemplate.from_messages(
            [
                ("system", "Extract expense details. Return null if unknown."),
                ("human", "{text}")
            ]
        )

        api_key = os.getenv("MISTRAL_API_KEY")
        if not api_key:
            raise ValueError("MISTRAL_API_KEY not found in .env")

        self.llm = ChatMistralAI(
            api_key=api_key,
            model="mistral-large-latest"
        )

        self.runnable = self.prompt | self.llm.with_structured_output(Expense)

    def runLLM(self, message):
        print("Calling LLM...")
        result = self.runnable.invoke({"text": message})
        print("LLM Response:", result)
        return result
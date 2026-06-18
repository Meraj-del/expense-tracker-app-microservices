from pydantic import BaseModel, Field
from typing import Optional

class Expense(BaseModel):
    amount: Optional[float] = Field(default=None)
    merchant: Optional[str] = Field(default=None)
    currency: Optional[str] = Field(default=None)
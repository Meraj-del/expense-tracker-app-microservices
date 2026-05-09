import re
import logging

logger = logging.getLogger(__name__)

class MessageUtil:
    def isBankSms(self, message):
        logger.info("Checking keywords in message...")
        words_to_search = ['spent', 'card', 'bank']
        pattern = r'\b(?:' + '|'.join(map(re.escape, words_to_search)) + r')\b'
        match = bool(re.search(pattern, message, flags=re.IGNORECASE))
        logger.info("Match found: %s", match)
        return match
import re

class MessageUtil:
    def isBankSms(self, message):
        print("Checking keywords in message...")
        words_to_search = ['spent', 'card', 'bank']
        pattern = r'\b(?:' + '|'.join(map(re.escape, words_to_search)) + r')\b'
        match = bool(re.search(pattern, message, flags=re.IGNORECASE))
        print("Match found:", match)
        return match
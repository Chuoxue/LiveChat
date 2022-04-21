from bot_module.trie import Trie

class sensitive(object):
    def __init__(self, path):
        self.trie = Trie()
        self.build(path)
        print("Sensitive module init over")

    def insert_new(self, word_list):
        word_list = [word.lower() for word in word_list]
        self.trie.insert(word_list)

    def build(self, path):
        f = open(path, "r", encoding="utf-8")
        for line in f:
            line = line.strip()
            if line:
                self.insert_new(line)
    
    def enumerateMatchList(self, word_list):
        word_list = [word.lower() for word in word_list]
        match_list = self.trie.enumerateMatch(word_list)
        return match_list
    
    def check(self, query):
        al = set()
        length = 0
        for indx in range(len(query)):
            index = indx + length
            match_list = self.enumerateMatchList(query[index:])
            if match_list == []:
                continue
            else:
                match_list = max(match_list)
                length = len("".join(match_list))
                al.add(match_list)
        if len(al) == 0:
            return False
        else:
            return True




# class sensitive():
#     trie = Trie()
    
#     def __init__(self):
#         root_dir = os.path.dirname(os.path.abspath(__file__))
#         sensitive_file = open(root_dir + "\data\敏感词.txt", "r", encoding="utf-8")

#         #insert sensitive word into trie
#         while 1:
#             line = sensitive_file.readline()
#             if not line:
#                 break
#             line = line.strip()
#             try:
#                 trie.insert(line)
#             except:
#                 print(line)    
            
#         sensitive_file.close()

#     def check(input):
        
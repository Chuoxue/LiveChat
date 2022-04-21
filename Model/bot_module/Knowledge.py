from bot_module.trie import Trie

class kbm(object):
    def __init__(self, path):
        self.kbm_map = {}
        self.trie = Trie()
        self.build(path)
        print('knowledge base init over')

    def insert_new(self, word_list):
        word_list = [word.lower() for word in word_list]
        self.trie.insert(word_list)

    def build(self, path):
        f = open(path, "r", encoding="utf-8")
        for line in f:
            line = line.strip()
            if line:
                line_list = line.split('\t')
                self.insert_new(line_list[0])
                self.kbm_map[line_list[0]] = line_list[1]
    
    def enumerateMatchList(self, word_list):
        word_list = [word.lower() for word in word_list]
        match_list = self.trie.enumerateMatch(word_list)
        return match_list
    
    def match(self, query):
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
        if(len(al) == 0):
            return " "
        else:
            return self.kbm_map[al.pop()]
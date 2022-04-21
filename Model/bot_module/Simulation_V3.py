import numpy as np
import pandas as pd
import os
import pickle

import faiss
from faiss import normalize_L2


class Simulation:
    def __init__(self, corpuses, commonEncoder, init, fileType="csv"):
        self.root_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        self.encoder = commonEncoder
        self.sentences = []
        self.index = []
        
                
        for i in range(len(corpuses)):
            corpus = corpuses[i]
            
            if fileType == "txt" and i == len(corpuses)-1:
                s = []
                with open(self.root_dir + '/data/' + corpus+'.txt', "r", encoding="utf-8") as f:
                    for line in f:
                        s.append(line.strip())
                self.sentences.append(s)
            else:
                df = pd.read_csv(self.root_dir + '/data/' + corpus + '.csv', encoding="utf-8")
                self.sentences.append(df.S.to_list())
            
            
            if(i == len(corpuses)-1 and not init):
                sentences_emb = np.array(self.encoder.encode(self.sentences[i:i+1]))
                normalize_L2(sentences_emb)
                
                #保存成为pickle文件
                f=open(self.root_dir + '/data/' + corpus + '.pickle','wb')  #以二进制的方式打开
                pickle.dump(sentences_emb, f)
                f.close()
            else:
                f=open(self.root_dir + '/data/' + corpus + '.pickle','rb')  #以二进制的方式打开
                sentences_emb=pickle.load(f)
                f.close()
            
            nlist = 1
            d = sentences_emb.shape[1] 
            quantizer = faiss.IndexFlatL2(d)
            _index = faiss.IndexIVFFlat(quantizer, d, nlist, faiss.METRIC_INNER_PRODUCT)  # build the index
            
            _index.train(sentences_emb)
            _index.add(sentences_emb)                  # add vectors to the index
            self.index.append(_index)

    def findNearest(self, query, threshold, corpusID=0):
        query_emb = np.array(self.encoder.encode([query]))
        print(query_emb.shape)
        normalize_L2(query_emb)
        k=1
        # 未处理好之前不允许访问
        if(corpusID >= self.index.__len__()):
            return "此语料未初始化完成"
        D,I = self.index[corpusID].search(query_emb, k)
        target = I[0][0]
        print("相似度：", D[0][0])
        print("语言模拟模块:", self.sentences[corpusID][target])        
        if D[0][0] > threshold:
            return self.sentences[corpusID][target]
        else:
            return ""


# '''
# sentences: [['aaaa'],['bb bbb'],['cccc cc ccc']]
# query: [['ddd ddd']]

# '''

# F = FindNearest('aaa')
# target = F.findNearest('嗡嗡嗡')
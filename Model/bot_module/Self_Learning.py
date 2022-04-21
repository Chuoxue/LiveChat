from transformers import AutoTokenizer, AutoModelForMaskedLM, BertForNextSentencePrediction,BertTokenizer
from bot_module.BertClassification import *
import torch
import pandas as pd
import threading
import os

import faiss
from faiss import normalize_L2

class Self_Learning():
    def __init__(self, commonEncoder):
        self.encoder = commonEncoder 
        self.tokenizer = BertTokenizer.from_pretrained("bert-base-chinese")
        self.model = BertForNextSentencePrediction.from_pretrained("bert-base-chinese")
        self.classification = Classification()
        self.count_h = 0
        self.count_s = 0
        
        self.root_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

    #对长句子效果较好，短句效果不太行
    def check(self, s1, s2):
        encoding = self.tokenizer.encode_plus(s1, s2, return_tensors='pt')
        logits = self.model(encoding['input_ids'], token_type_ids=encoding['token_type_ids'])[0]
        res = logits[0][0] > logits[0][1]
        
        if(int(res) == 1):
            return True
        else:
            return False
    
    def update(self, s1, s2):
        if(self.check(s1, s2) == False):
            print("自学习模块：两者间无上下句关系")
            return -1
        
        #将s2带入分类器中，判断属于哪种语料
        flag = self.classification.check(inputs = s2)
        corpus = ""
        if(flag == -1):
            print("未能匹配与心情语料")
            return -1
        elif(flag == 0):
            corpus = "开心语料库.csv"
            self.count_h += 1
        else:
            corpus = "沮丧语料库.csv"
            self.count_s += 1
        
        print("自学习拓展：", corpus)
        df = pd.read_csv(self.root_dir + "/data/" + corpus)
        new=pd.DataFrame({'S':s2}, index=[1])
        df = df.append(new, ignore_index=True)
        df.to_csv(self.root_dir + "/data/" +corpus, index=0, encoding='utf-8')
        
        #TODO:往语料相似库中添加 (以下部分待验证！) (默认不开启)
        if(self.count_h >= 100 or self.count_s >= 100): #每自学习100个集体添加一次，避免浪费
            corpus = ""
            if(self.count_h >= 100):
                corpus = "开心语料库"
            else:
                corpus = "沮丧语料库"
            df = pd.read_csv(self.root_dir + "/data/" + corpus + ".csv")
            
            # 对新数据进行embedding
            newSentences = df.S.tolist()[-100:]
            sentences_emb_new = np.array(self.encoder.encode(newSentences))
            normalize_L2(sentences_emb_new)
            # 和原embedding数据进行合并
            f=open(self.root_dir + "/data/" + corpus + '.pickle','rb')  #以二进制的方式打开
            sentences_emb_old=pickle.load(f)
            f.close()
            sentences_emb = np.concatenate((sentences_emb_old, sentences_emb_new), axis=0)     
            # 带入fassi库中
            nlist = 1
            d = sentences_emb.shape[1]
            quantizer = faiss.IndexFlatL2(d)
            index = faiss.IndexIVFFlat(quantizer, d, nlist, faiss.METRIC_INNER_PRODUCT)  # build the index
            
            index.train(sentences_emb)
            index.add(sentences_emb)
            
        return 1
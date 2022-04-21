# from bot_module.Simulation_V3 import Simulation

# sim = Simulation(corpus)

import numpy as np
import pandas as pd
import os
from sentence_transformers import SentenceTransformer
import pickle

import faiss
from faiss import normalize_L2

corpus = "柯南"

root_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
encoder = SentenceTransformer("symanto/sn-xlm-roberta-base-snli-mnli-anli-xnli")

df = pd.read_csv(root_dir + '/FinalDemo/data/' + corpus + '.csv')
sentences = df.S.to_list()
sentences_emb = np.array(encoder.encode(sentences))
normalize_L2(sentences_emb)  


f=open(root_dir + '/FinalDemo/data/' + corpus + '.pickle','wb')  #以二进制的方式打开
pickle.dump(sentences_emb, f)
f.close()
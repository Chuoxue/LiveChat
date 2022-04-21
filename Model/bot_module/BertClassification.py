import pandas as pd
from transformers import AutoTokenizer, BertForSequenceClassification
import torch
from torch import nn
from torch import optim
import math
import os

class BertClassificationModel(nn.Module):
    def __init__(self):
        super(BertClassificationModel, self).__init__()   
        model_class, tokenizer_class, pretrained_weights = (BertForSequenceClassification, AutoTokenizer, 'bert-base-chinese')         
        self.tokenizer = tokenizer_class.from_pretrained(pretrained_weights)
        self.bert = model_class.from_pretrained(pretrained_weights)
        self.dense = nn.Linear(768, 2)  #bert默认的隐藏单元数是768， 输出单元是2，表示二分类
        
    def forward(self, batch_sentences):
        batch_tokenized = self.tokenizer.batch_encode_plus(batch_sentences, add_special_tokens=True,
                                pad_to_max_length=True, truncation=True)      #max_len=66,
        input_ids = torch.tensor(batch_tokenized['input_ids'])
        attention_mask = torch.tensor(batch_tokenized['attention_mask'])
        bert_output = self.bert(input_ids, attention_mask=attention_mask)
        bert_cls_hidden_state = bert_output[0][:,0,:]       #提取[CLS]对应的隐藏状态
        linear_output = self.dense(bert_cls_hidden_state)
        return linear_output

class Classification:
    def __init__(self):
        self.bert_classifier_model = torch.load(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))+'/data/model.pkl', map_location=torch.device('cpu'))
        self.bert_classifier_model.to('cpu')
        
    def check(self, inputs):
        with torch.no_grad():
            outputs = self.bert_classifier_model([inputs])
            if float(torch.softmax(outputs, 1)[0][0]) < 0.8 and float(torch.softmax(outputs, 1)[0][1]) < 0.8:
                return -1 #未匹配成功，不能加入语料库中
            _, predicted = torch.max(outputs, 1)
            return predicted
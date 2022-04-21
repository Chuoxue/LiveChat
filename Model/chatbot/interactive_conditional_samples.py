import torch
import torch.nn.functional as F
from tokenization_unilm import UnilmTokenizer
from modeling_unilm import UnilmForSeq2SeqDecodeSample, UnilmConfig
import copy
import os
import argparse
import re

class Chatbot:
    def __init__(self, model_name_or_path = os.path.dirname(os.path.abspath(__file__)) + "/robot_model"):  
        self.device = 'cuda' if torch.cuda.is_available() else 'cpu'
        
        print('using device:{}'.format(self.device))
        os.environ['CUDA_DEVICE_ORDER'] = 'PCI_BUS_ID'
        os.environ["CUDA_VISIBLE_DEVICES"] = self.device
        
        self.config = UnilmConfig.from_pretrained(model_name_or_path, max_position_embeddings=512)
        self.tokenizer = UnilmTokenizer.from_pretrained(model_name_or_path, do_lower_case=False)
        self.model = UnilmForSeq2SeqDecodeSample.from_pretrained(model_name_or_path, config=self.config)
        self.model.to(self.device)
        self.model.eval()
        print('Chatbot init over')


    def remove_multi_symbol(self, text):
        r = re.compile(r'([.,，/\\#!！？?。$%^&*;；:：{}=_`´︵~（）()-])[.,，/\\#!！？?。$%^&*;；:：{}=_`´︵~（）()-]+')
        text = r.sub(r'\1', text)
        return text


    def top_k_top_p_filtering(self, logits, top_k=0, top_p=0.0, filter_value=-float('Inf')):
        assert logits.dim() == 1
        top_k = min(top_k, logits.size(-1))
        if top_k > 0:
            indices_to_remove = logits < torch.topk(logits, top_k)[0][..., -1, None]
            logits[indices_to_remove] = filter_value
        if top_p > 0.0:
            sorted_logits, sorted_indices = torch.sort(logits, descending=True)
            cumulative_probs = torch.cumsum(F.softmax(sorted_logits, dim=-1), dim=-1)
            sorted_indices_to_remove = cumulative_probs > top_p
            sorted_indices_to_remove[..., 1:] = sorted_indices_to_remove[..., :-1].clone()
            sorted_indices_to_remove[..., 0] = 0
            indices_to_remove = sorted_indices[sorted_indices_to_remove]
            logits[indices_to_remove] = filter_value
        return logits


    """
    max_len: 生成的对话的最大长度
    topk: 取前k个词
    topp: 取超过p的词
    repetition_penalty: 重复词的惩罚项
    """
    def input(self, text, max_len=32, topk=2, topp=0.95, repetition_penalty=1.2):
        try:
            input_ids = self.tokenizer.encode(text)
            token_type_ids = [4] * len(input_ids)
            generated = []
            for _ in range(max_len):
                curr_input_ids = copy.deepcopy(input_ids)
                curr_input_ids.append(self.tokenizer.mask_token_id)
                curr_input_tensor = torch.tensor(curr_input_ids).long().to(self.device).view([1, -1])
                curr_token_type_ids = copy.deepcopy(token_type_ids)
                curr_token_type_ids.extend([5])
                curr_token_type_ids = torch.tensor(curr_token_type_ids).long().to(self.device).view([1, -1])
                
                outputs = self.model(input_ids=curr_input_tensor, token_type_ids=curr_token_type_ids, attention_mask=None)
                next_token_logits = outputs[-1, -1, :]
                for id in set(generated):
                    next_token_logits[id] /= repetition_penalty
                next_token_logits[self.tokenizer.convert_tokens_to_ids('[UNK]')] = -float('Inf')
                filtered_logits = self.top_k_top_p_filtering(next_token_logits, top_k=topk, top_p=topp)
                next_token = torch.multinomial(F.softmax(filtered_logits, dim=-1), num_samples=1)
                if next_token == self.tokenizer.sep_token_id:  # 遇到[SEP]则表明生成结束
                    break
                generated.append(next_token.item())
                input_ids.append(next_token.item())
                token_type_ids.extend([5])
            text = self.tokenizer.convert_ids_to_tokens(generated)
            text = self.remove_multi_symbol("".join(text))
            return text
        except:
            return "说点别的吧，好吗？"


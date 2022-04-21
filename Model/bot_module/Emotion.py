import pandas as pd
import os
from bot_module.Simulation_V3 import Simulation
import random

class Emotion:
    def __init__(self, commonEncoder, happy="开心语料库.csv", sad="沮丧语料库.csv"):
        self.sim_h = Simulation([happy], commonEncoder, True)
        self.sim_s = Simulation([sad], commonEncoder, True)
        
    def match(self, query, p_emotion, threshold=0.9):
        state = -1
        if random.random() < p_emotion:
            print("匹配开心语料库")
            state = 0
            _output = self.sim_h.findNearest(query, threshold)
        else:
            print("匹配沮丧语料库")
            state = 1
            _output = self.sim_s.findNearest(query, threshold)
        
        #-1:未匹配，0：开心语料，1：沮丧语料
        if(len(_output) == 0):
            return state, query
        else:
            return state, _output
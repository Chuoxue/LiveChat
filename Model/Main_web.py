# 合理增加多线程
# 加入默认语音

import html
import web
import shutil
from bot_module.Sensitive import sensitive
from bot_module.Knowledge import kbm
from bot_module.Simulation_V3 import Simulation
from bot_module.Self_Learning import *
from bot_module.Emotion import Emotion
from sentence_transformers import SentenceTransformer


from Voice import Voice

import threading
import os
import sys
import json

urls = (
    '/register/(.*)/(.*)', 'registerHandle', #parameter：用户名+密码
    '/login/(.*)/(.*)', 'loginHandle', #parameter：用户名+密码
    '/uploadAudio/(.*)', 'uploadAudio', #parameter：用户名
    '/uploadCorpus/(.*)', 'uploadCorpus', #parameter：用户名
    '/download/(.*)/audio.wav', 'download', #parameter：用户名
    '/Home', 'Home',
    '/updateConfig/(.*)/(.*)/(.*)/(.*)/(.*)/(.*)', 'updateConfig', 
    #parameter：用户名 + init[0/1] + threshold_c[0~1] + threshold_e[0~1] + 
    #emotion_preference[0~1] + thumbs_up[0/1]
    '/(.*)/(.*)/(.*)/(.*)/(.*)', 'start', 
    #parameter：用户名+上次服务器返回+对话内容+人物语料ID[0~n] + 是否使用语音回复[0, 1]
)
app = web.application(urls, globals())

isLogin = False

# 参数初始化
root_dir = os.path.dirname(os.path.abspath(__file__))
#————————————————————————————————
sensitive_checker = sensitive(root_dir + "/data/敏感词.txt")
#————————————————————————————————
kbm = kbm(root_dir + "/data/知识库.txt")
#————————————————————————————————
sys.path.append(root_dir + "/chatbot")
from interactive_conditional_samples import Chatbot
chatbotmachine = Chatbot()
#————————————————————————————————
corpusList = ['柯南', '哆啦A梦']
encoder = SentenceTransformer('symanto/sn-xlm-roberta-base-snli-mnli-anli-xnli')
sim = Simulation(corpusList, encoder, True)
#————————————————————————————————
emo = Emotion(encoder, "开心语料库", "沮丧语料库")
#————————————————————————————————
sl = Self_Learning(encoder)
#————————————————————————————————
voice = Voice()

# p_emotion = 0.5 #开心语料库的概率
last_output = "你好啊。"
input = "你好啊，今天你吃了吗？"
corpusIdx = 1
#————————————————————————————————

class Home:
    def GET(self):
        file = open(root_dir + '/index.html', 'r', encoding='utf-8')
        content = file.read()
        file.close()
        return content


class registerHandle:
    def GET(self, account_new, password_new):
        code_new = str(hash(account_new)+1)
        with open(root_dir + "/data/database", mode='r+', encoding='utf-8') as lines:
            for line in lines:
                line = line.strip().split(" ")
                account = line[0]
                if account == account_new:
                    return 0 #注册失败
                    # return 'Register False'
            lines.write(account_new+' '+ password_new + '\n')
            
        newDir(root_dir + "/data/account/" + account_new)
        shutil.copy(root_dir + "/data/audio.wav", root_dir + "/data/account/"+account_new+"/audio.wav")
        data = {
                'threshold_character':0.75, 
                'threshold_emotion':0.75, 
                "emotion_preference":0.5
                }
        with open(root_dir+"/data/account/"+account_new+"/config.json", "w") as fp:
            json.dump(data, fp, indent=4)
        
        return 1 #注册成功
        # return "Register success"

class loginHandle:
    def GET(self, account_new, password_new):
        isLogin = False
        print(root_dir + "/data/database")
        with open(root_dir + "/data/database", mode='r', encoding="utf-8") as lines:
            for line in lines:
                line = line.strip().split(" ")
                account = line[0]
                password = line[1]
                if account == account_new and password == password_new:
                    isLogin = True
                    break
            
        if isLogin == True:
            return 1 #"登陆成功"
        else:
            return 0 #"登陆失败"


class updateConfig:
    def GET(self, username, init, threshold_c, threshold_e, preference, thumbs_up):
        if init == 1:
            data = {
                    'threshold_character':threshold_c, 
                    'threshold_emotion':threshold_e, 
                    "emotion_preference":preference
                    }
            with open(root_dir+"/data/account/"+username+"/config.json", "w") as fp:
                json.dump(data, fp, indent=4)
            return 1
        else:
            with open(root_dir+"/data/account/"+username+"/config.json", "r") as fp:
                config = json.load(fp)
            if(config["last_stage"] == "character"):
                if(thumbs_up == "1"):
                    config["threshold_character"] -= 0.005
                else:
                    config["threshold_character"] += 0.005
            elif(config["last_stage"] == "emotion"):
                last_state = config["last_state"]
                #在未匹配的情况下，点赞：增加阈值；点踩：降低阈值
                if(last_state == "-1"):
                    if(thumbs_up == "1"):
                        config["threshold_emotion"] += 0.005
                    else:
                        config["threshold_emotion"] -= 0.005
                #在匹配开心语料的情况下，点赞：提升偏好概率；点踩：降低偏好概率
                elif(last_state == "0"):
                    if(thumbs_up == "1"):
                        config["emotion_preference"] += 0.005
                    else:
                        config["emotion_preference"] -= 0.005
                #在匹配沮丧语料的情况下，点赞：降低偏好概率；点踩：提升偏好概率
                else:
                    if(thumbs_up == "1"):
                        config["emotion_preference"] -= 0.005
                    else:
                        config["emotion_preference"] += 0.005
            else:
                #在一些没有意义的情况下进行了点赞、踩操作
                return 0
            
            #重新保存
            with open(root_dir+"/data/account/"+username+"/config.json", "w") as fp:
                json.dump(config, fp, indent=4)
            return 1

class uploadAudio:
    def GET(self, username):
        file = open(root_dir + '/audio.html', 'r', encoding='utf-8')
        content = file.read()
        file.close()
        return content

    def POST(self, username):
        x = web.input(stblog={})
        filename = html.unescape(x['stblog'].filename)
        web.debug(filename) # 这里是文件名
        # web.debug(x['stblog'].value) # 这里是文件内容
        
        fout = open(root_dir +'/data/account/' + username + '/audio.wav','wb') # creates the file where the uploaded file should be stored
        fout.write(x.stblog.file.read()) # writes the uploaded file to the newly created file.
        fout.close() # closes the file, upload complete.
        
        # raise web.seeother('/uploadAudio/muwu')
        file = open(root_dir + '/success.html', 'r', encoding='utf-8')
        content = file.read()
        file.close()
        return content


class uploadCorpus:
    def GET(self, username):
        file = open(root_dir + '/corpus.html', 'r', encoding='utf-8')
        content = file.read()
        file.close()
        return content


    def POST(self, username):
        x = web.input(stblog={})
        filename = html.unescape(x['stblog'].filename)
        web.debug(filename) # 这里是文件名
                
        fout = open(root_dir +'/data/' + filename,'wb') # creates the file where the uploaded file should be stored
        fout.write(x.stblog.file.read()) # writes the uploaded file to the newly created file.
        fout.close() # closes the file, upload complete. 

        global corpusIdx
        corpusIdx += 1
        fileType = "".join(filename.split('.')[-1])
        filename =  "".join(filename.split('.')[0:-1])
        
        #在此处开启新线程
        t = threading.Thread(target=self.processingCorpus, args=(filename, fileType))
        t.start()
        
        return corpusIdx
    
    def processingCorpus(_, filename, fileType):
        print(filename)
        global corpusList
        corpusList.append(filename) #添加新的人物语料
        print(corpusList)

        #重新构建人物相似度库
        global sim
        sim = Simulation(corpusList, encoder, False, fileType)


class download:
    def GET(self, account):
        fout = open(root_dir +'/data/account/' + account + '/output.wav','rb') # creates the file where the uploaded file should be stored
        out = fout.read()
        fout.close() # closes the file, upload complete.
        return out

    def POST(self, account):
        fout = open(root_dir +'/data/account/' + account + '/output.wav','rb') # creates the file where the uploaded file should be stored
        out = fout.read()
        fout.close() # closes the file, upload complete.
        return out


class start:
    def GET(self, account, last_output, sentence, corpusID, _isVoice):
        isVoice = False
        if(_isVoice == "1"):
            isVoice = True
        originalImitatedAudioPath = root_dir +'/data/account/' + account + '/audio.wav'
        if(os.path.exists(originalImitatedAudioPath) == False):
            originalImitatedAudioPath = root_dir + '/data/audio.wav'
        web.header('Content-Type', 'text/html;charset=UTF-8')
        
        try:
            #初始化
            print("last sentence:", last_output)
            print("Sentence is:", sentence)
            input = sentence
            
            with open(root_dir+"/data/account/"+account+"/config.json", "r") as fp:
                config = json.load(fp)
            p_emotion = config["emotion_preference"]
            threshold_c = config["threshold_character"]
            threshold_e = config["threshold_emotion"]
            
            
            #敏感词汇过滤模块
            if(sensitive_checker.check(input)):
                _ouput = "我不喜欢聊这个话题。o(≧口≦)o"
                
                config['last_stage'] = "None"
                with open(root_dir+"/data/account/"+account+"/config.json", "w") as fp:
                    json.dump(config, fp, indent=4)
                
                if(isVoice):
                    voice.imitation("我不喜欢聊这个话题。", originalImitatedAudioPath)
                return _ouput


            #知识库模块
            _output = kbm.match(input)
            if(_output != " "):
                print("知识库模块匹配成功：", _output)
                
                config['last_stage'] = "None"
                with open(root_dir+"/data/account/"+account+"/config.json", "w") as fp:
                    json.dump(config, fp, indent=4)
                
                if(isVoice):
                    voice.imitation(_output, originalImitatedAudioPath)
                return _output


            #对话模块
            output = chatbotmachine.input(input)
            print("原输出：", output)


            #自学习模块(创建一个新的线程运行)
            if(last_output != " "):
                thread=threading.Thread(target=sl.update,args=(last_output, input))
                thread.start()


            #语言模拟模块(默认柯南语料) [ID为-1的时候表示不开启此功能]
            if(corpusID != "-1"):
                _output = sim.findNearest(output, threshold_c, int(corpusID))
                if(len(_output) != 0):
                    print("语言模拟模块匹配成功：", _output)
                    
                    config['last_stage'] = "character"
                    with open(root_dir+"/data/account/"+account+"/config.json", "w") as fp:
                        json.dump(config, fp, indent=4)
                    
                    if(isVoice):
                        voice.imitation(_output, originalImitatedAudioPath)
                    return _output

            #情感模块
            state, output = emo.match(output, float(p_emotion), threshold_e)
            print("最终输出：", output)
            
            #最后判断机器人回复中是否存在敏感词
            if(sensitive_checker.check(output)):
                _ouput = "我不喜欢聊这个话题。o(≧口≦)o"
                
                config['last_stage'] = "None"
                with open(root_dir+"/data/account/"+account+"/config.json", "w") as fp:
                    json.dump(config, fp, indent=4)
                
                if(isVoice):
                    voice.imitation("我不喜欢聊这个话题。", originalImitatedAudioPath)
                return _ouput
            
            
            config['last_stage'] = "emotion"
            config['last_state'] = state
            with open(root_dir+"/data/account/"+account+"/config.json", "w") as fp:
                json.dump(config, fp, indent=4)
            
            if(isVoice):
                voice.imitation(output, originalImitatedAudioPath)
                print("语音模拟结束！！！")
            print("--------------------------------------------------")
            return output
    
        except Exception as e:
            print("chatbot ERROR: ", e)
            if(isVoice):
                voice.imitation("我不想谈论这个，换个话题吧", originalImitatedAudioPath)
            print("--------------------------------------------------")
            return '我不想谈论这个，换个话题吧o((>ω< ))o'



def newDir(dir_path):
    if not os.path.exists(dir_path):
        os.makedirs(dir_path)

if __name__ == "__main__":
    app.run()
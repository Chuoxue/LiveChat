from pathlib import Path
from synthesizer.inference import Synthesizer
from encoder import inference as encoder
from vocoder.hifigan import inference as gan_vocoder
from vocoder.wavernn import inference as rnn_vocoder
import numpy as np
import re
from scipy.io.wavfile import write
import librosa
import io
import base64
import os

class Voice:
    def __init__(self):
        syn_models_dirt = "synthesizer/saved_models"
        synthesizers = list(Path(syn_models_dirt).glob("**/*.pt"))
        synthesizers_cache = {}
        encoder.load_model(Path("encoder/saved_models/pretrained.pt"))
        gan_vocoder.load_model(Path("vocoder/saved_models/pretrained/g_hifigan.pt"))

        # Load synthesizer
        synt_path = synthesizers[0]
        print("NO synthsizer is specified, try default first one.")

        if synthesizers_cache.get(synt_path) is None:
            self.current_synt = Synthesizer(Path(synt_path))
            synthesizers_cache[synt_path] = self.current_synt
        else:
            self.current_synt = synthesizers_cache[synt_path]
        print("using synthesizer model: " + str(synt_path))


    def imitation(self, text, wavPath="data/audio.wav"):
        # Load input wav
        wav, sample_rate = librosa.load(wavPath)

        # write("temp.wav", sample_rate, wav) #Make sure we get the correct wav

        encoder_wav = encoder.preprocess_wav(wav, sample_rate)
        embed, _, _ = encoder.embed_utterance(encoder_wav, return_partials=True)

        # Load input text
        texts = [text]
        punctuation = '！，。、,' # punctuate and split/clean text
        processed_texts = []
        for text in texts:
            for processed_text in re.sub(r'[{}]+'.format(punctuation), '\n', text).split('\n'):
                if processed_text:
                    processed_texts.append(processed_text.strip())
        texts = processed_texts
        print(texts)

        # synthesize and vocode
        embeds = [embed] * len(texts)
        specs = self.current_synt.synthesize_spectrograms(texts, embeds)
        spec = np.concatenate(specs, axis=1)
        # wav = rnn_vocoder.infer_waveform(spec)
        wav = gan_vocoder.infer_waveform(spec)

        # Return cooked wav
        out = io.BytesIO()
        write(out, Synthesizer.sample_rate, wav)
        # return Response(out, mimetype="audio/wav")

        filedir = os.path.dirname(wavPath)
        filename = "output.wav"
        fout = open(filedir +'/'+ filename,'wb') # creates the file where the uploaded file should be stored
        fout.write(out.read()) # writes the uploaded file to the newly created file.
        fout.close() # closes the file, upload complete.
        
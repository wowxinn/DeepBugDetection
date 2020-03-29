
# coding: utf-8

# ## vocabhandler

import sys
import codecs
from collections import Counter
from operator import itemgetter


# In[12]:


vocab_output = "model_seq/vocab.txt" # 产出词表


# ### 读取词表，定义获取编号id的方法

# In[21]:

vocab = []
word_to_idx = []
def load_vocab():
    global vocab 
    global word_to_idx
    with codecs.open(vocab_output, "r", "utf-8") as f:
        vocab = [w.strip() for w in f.readlines()]
    word_to_idx = {k : v for (k,v) in zip(vocab,range(len(vocab)))}
    print("[INFO]loading vocabulary...done.")
    # print(word_to_idx)


# In[ ]:

def get_word(id_):
    global vocab 
    global word_to_idx
    return vocab[id_] if id_ > 0 and id_ < len(vocab) else "UNK"

# 如果出现低频词，替换为"<unk>"
def get_id(word):
    global vocab 
    global word_to_idx
    return word_to_idx[word] if word in word_to_idx else word_to_idx["UNK"]
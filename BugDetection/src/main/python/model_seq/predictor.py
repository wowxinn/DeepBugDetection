
# coding: utf-8

# ## Predictor

# In[1]:


import os
os.environ["CUDA_VISIBLE_DEVICES"] = "0"
import tensorflow as tf
from tensorflow.contrib.rnn import LSTMCell, LSTMStateTuple
from tensorflow.python.util import nest
from sklearn.utils import shuffle

import numpy as np
from model_seq.vocab import Vocab
import os
import time
import zerorpc
import pickle

print(tf.__version__)
# In[2]:


VOCAB_SIZE = 1564
VOCAB_PATH = 'model_seq/vocab.txt'
MODEL_PATH = 'model_seq/model/o=sgd_v=1564h=250l=2b=64e=20lr=0.002/model-1'

HIDDEN_SIZE = 250
lr=0.002
KEEP_PROB = 1 
NUM_LAYER = 2
EMBEDDING_LEARNING_RATE_FACTOR = 0.1
TOPN = 50


# In[3]:


def get_batch(dataset,labelset,batchsize):
    
    total = int(len(dataset)/batchsize)
    if len(dataset)%batchsize > 0:
        total += 1
    for i in range(total):
        tempdata = dataset[i*batchsize:(i+1)*batchsize]
        templabel = labelset[i*batchsize:(i+1)*batchsize]
        sequence_length = [len(seq) for seq in tempdata]
        yield tempdata,templabel

def batch_major(inputs, max_sequence_length=None):
    
    sequence_lengths = [len(seq) for seq in inputs]
    batch_size = len(inputs)
    
    if max_sequence_length is None:
        max_sequence_length = max(sequence_lengths)
    
    inputs_batch_major = np.zeros(shape=[batch_size, max_sequence_length], dtype=np.int32) # == PAD
    
    for i, seq in enumerate(inputs):
        for j, element in enumerate(seq):
            inputs_batch_major[i,j] = element
    
    return inputs_batch_major, sequence_lengths

def read_weight_matrix():
    weight_matrix = np.random.uniform(-0.05,0.05,(VOCAB_SIZE,50)).astype(np.float32)
    return weight_matrix


# In[4]:


class SeqLSTM(object):

    def __init__(self):
        self.vocab = Vocab(VOCAB_PATH)
        self.weight_matrix = read_weight_matrix()
        self.logits,self.top_n = self.get_session()
        self.saver = tf.train.Saver()
        self.sess = self.restore_graph()
        print('Now Serving...')

    def restore_graph(self):
        checkpoint = MODEL_PATH
        sess = tf.InteractiveSession()
        print('restoring params...')
        self.saver.restore(sess,checkpoint)
        print('get session completed!')
        return sess

    def get_session(self):
        print('prepare model...')
        tf.reset_default_graph()

        self.inputs = tf.placeholder(shape=(None, None), dtype=tf.int32, name='inputs')
        self.inputs_length = tf.placeholder(shape=(None,), dtype=tf.int32, name='inputs_length')
        self.labels = tf.placeholder(shape=(None,), dtype=tf.int32, name='labels')
 
        self.cell = tf.contrib.rnn.MultiRNNCell([tf.contrib.rnn.BasicLSTMCell(HIDDEN_SIZE) for _ in range(NUM_LAYER)])

        self.embedding = tf.Variable(initial_value=self.weight_matrix,dtype=tf.float32)

        #batch_size = BATCH_SIZE

        embedding = tf.nn.embedding_lookup(self.embedding,self.inputs)
        
        # dropout
        embedding = tf.nn.dropout(embedding,KEEP_PROB)
        
        with tf.variable_scope("dynamic_rnn"):
            outputs, last_state = tf.nn.dynamic_rnn(self.cell, embedding, self.inputs_length, dtype=tf.float32)

            newoutputs = last_state[-1][1]
        
        #logits = tf.matmul(newoutputs,self.softmax_weight) + self.softmax_bias
        logits = tf.layers.dense(newoutputs,VOCAB_SIZE)

        
        top_n = tf.nn.top_k(logits,TOPN)
        top_n_prob = top_n.values
        top_n_prob = tf.nn.softmax(top_n_prob)

        loss = tf.nn.sparse_softmax_cross_entropy_with_logits(logits=logits, labels=self.labels)

        cost = tf.reduce_mean(loss)

        trainable_variables = tf.trainable_variables()

        # opt = tf.train.AdamOptimizer(learning_rate=lr)
        opt = tf.train.GradientDescentOptimizer(learning_rate=lr)
        grads_and_vars = opt.compute_gradients(loss) 
        clip_grads = [(tf.clip_by_value(grad,-1.,1.),var) for grad,var in grads_and_vars if grad is not None]

        found = 0
        for i,(grad,var) in enumerate(clip_grads):
            if var == self.embedding:
                found += 1
                grad = tf.scalar_mul(EMBEDDING_LEARNING_RATE_FACTOR, grad)
                clip_grads[i] = (grad,var)
        assert found == 1# internal consistency check

        train_op = opt.apply_gradients(clip_grads)
        self.saver = tf.train.Saver()
        return logits,top_n

    
    def predict(self,usr_seq):
        dataset = usr_seq
        sizedata = len(dataset)
        end = sizedata
        for i,(batchInputs,_) in enumerate(get_batch(dataset[0:end],[0],1),1):

            seq,length = batch_major(batchInputs)
            class_top_n = self.sess.run(self.top_n,feed_dict={self.inputs:seq,self.inputs_length:length})
             
            class_top_n_indices = class_top_n.indices
            class_top_n_values = self.sess.run(tf.nn.softmax(class_top_n.values.tolist()))
        return class_top_n_values, class_top_n_indices


# In[5]:


if(__name__=="__main__"):
    sim = SeqLSTM()
    sess = sim.sess
    print(sim.predict([[53, 23, 23, 117, 79, 46, 117]]))
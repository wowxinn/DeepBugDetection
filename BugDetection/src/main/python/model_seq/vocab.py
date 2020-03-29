#import tensorflow as tf

class Vocab(object):
	"""docstring for Vocab"""
	def __init__(self, path):
		self.size = 0
		self._index = {}
		self._tokens = []
		self.unk_index = None
		self.start_index = None
		self.end_index = None

		file = open(path)
		for line in file.readlines():
			if line == None:
				break
			else:
				line = line.replace('\r','')
				line = line.replace('\n','')
				line = line.replace(' ','')
				self.size += 1
				#self._tokens[self.size] = line
				#print line
				self._tokens.append(line)
				self._index[line] = self.size
				#print self._index[line]

		'''
		nunks = ['<unk>','<UNK>','UUUNKKK']
		for tok in nunks:
			if self.unk_index == None:
				self.unk_index = self._index[tok]
			else:
				self.unk_index = self.unk_index
			if self.unk_index != None:
				self.unk_token = tok


		starts = ['<s>','<S>']
		for tok in starts:
			if self.start_index == None:
				self.start_index = self._index[tok]
			else:
				self.start_index = self.start_index
			if self.start_index != None:
				self.start_token = tok


		ends = ['</s>','</S>']
		for tok in ends:
			if self.end_index == None:
				self.end_index = self._index[tok]
			else:
				self.end_index = self.end_index
			if self.end_index != None:
				self.end_token = tok	
		'''

	def contains(self,w):
#		if not self._index[w]:
#			return False
#		else:
#			return True
		return self._index.get(w,False)


	def add(self,w):
		ans = self.contains(w)
		if ans != False:
			return ans
		self.size += 1
		#self._tokens[self.size] = w
		self._tokens.append(w)
		self._index[w] = self.size
		return self.size

	def index(self,w):
		ans = self.contains(w)
		if ans != False:
			index = ans
			return index-1
		else:
		#	if self.unk_index == None:
		#		print 'Token not in vocabulary and no UNK token defined:' + w
		#
			return self._index['<unk>']-1


	def token(self,i):
		if (i+1) < 1 or (i+1)  > self.size:
			return "None"
		else:
			return self._tokens[i]


	def map(self,tokens):
		len = len(tokens)
		output = tf.IntTensor(len)
		for i in xrange(len):
			output[i] = self.index(tokens[i])
		return output


	def add_unk_token(self):
		if self.unk_token != None:
			return
		self.unk_token = self.add('<unk>')


	def add_start_token(self):
		if self.start_token != None:
			return
		self.start_index = self.add('<s>')


	def add_end_token(self):
		if self.end_token != None:
			return
		self.end_token = self.add('</s>')


package model.trie;


import java.util.Arrays;
import java.util.List;

/**
 * Trie
 *
 * 字典树, 用于存储 N-gram 解析出的词&词频
 *
 * @author wangxin
 */
public class Trie {

    TrieNode root;

    public Trie() {
        this.root = new TrieNode("");
    }

//    public void addSequence(String[] sequence){
//        assert sequence.length > 0;
//        TrieNode pointer = root;
//        for (int i = 0; i < sequence.length; i++) {
//            pointer = pointer.addChild(sequence[i]);
//        }
//    }

    public void addSequence(List<String> sequence){
        assert sequence.size() > 0;
        TrieNode pointer = root;
        root.addCount();
        for (int i = 0; i < sequence.size(); i++) {
            pointer = pointer.addChild(sequence.get(i));
        }
    }

    /**
     * 在Trie上寻找符合前缀的Prefix出现的数量
     * @param prefix : 前缀
     * @return : 返回前缀出现的数量
     * */
    public double getCountWithPrefix(List<String> prefix){
        TrieNode pointer = root;
        for (int i = 0; i < prefix.size() - 1; i++) {
            String word = prefix.get(i);
            TrieNode node = pointer.getChild(word);
            if(node.getCount() > 0){
                pointer = node;
            }
            else {
                pointer = new TrieNode("");
                break;
            }
        }
        return pointer.getChild(prefix.get(prefix.size()-1)).getCount();
    }

    /**
     * 在Trie上寻找符合前缀的TrieNode
     * @param prefix : 前缀
     * @return : 返回{@code TrieNode}表示符合前缀的最后一个节点, 如果count为0, 则说明没有该前缀存在
     * */
    public TrieNode getNodeWithPrefix(List<String> prefix){
        TrieNode pointer = root;
        for (int i = 0; i < prefix.size(); i++) {
            String word = prefix.get(i);
            TrieNode node = pointer.getChild(word);
            if(node.getCount() > 0){
                pointer = node;
            }
            else {
                pointer = new TrieNode("");
                break;
            }
        }
        return pointer;
    }

    @Override
    public String toString(){
        return root.toString();
    }
}

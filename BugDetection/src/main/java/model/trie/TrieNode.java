package model.trie;

import java.util.ArrayList;

/**
 * TrieNode
 *
 * 字典树上的节点
 * 记录:节点上的词, 整个 前缀+词 序列出现的次数
 *
 * @author wangxin
 */
public class TrieNode {

    // 序列出现次数
    private double count;
    // 词
    private String value;
    // 孩子节点
    private ArrayList<TrieNode> children;

    public TrieNode(String value) {
        this.value = value;
        this.count = 0d;
        this.children = new ArrayList<>();
    }

    /**
     * 向节点上增加孩子节点
     * @param word: 增加的词
     * @return : 返回 {@code TrieNode} 是树上对应这个词的节点
     * */
    public TrieNode addChild(String word){

        for (TrieNode child : this.children) {
            if(child.getValue().equals(word)){
                child.addCount();
                return child;
            }
        }

        TrieNode node = new TrieNode(word);
        node.addCount();
        this.children.add(node);

        return node;
    }

    public double getCount() {
        return count;
    }

    public void addCount() {
        this.count++;
    }

    public String getValue() {
        return value;
    }

    /**
     * 获取某个孩子节点
     * @param word: 获取的孩子节点表示的单词
     * @return : 返回找到的孩子节点, 如果没有找到, 初始化一个默认节点返回
     * */
    public TrieNode getChild(String word){
        for (TrieNode child : this.children) {
            if(child.getValue().equals(word)){
                return child;
            }
        }
        return new TrieNode(word);
    }

    public ArrayList<TrieNode> getChildren() {
        return children;
    }

    @Override
    public String toString(){
        return toStringWithPrefix("");
    }

    public String toStringWithPrefix(String prefix){
        StringBuilder sb = new StringBuilder();
        String oldprefix = prefix;
        if(!value.equals("")){
            sb.append(oldprefix).append(value).append(":").append(count).append("\n");
        }
        for (int i = 0; i < children.size(); i++) {
            prefix = oldprefix+value;
            sb.append(children.get(i).toStringWithPrefix(prefix));
        }
        return sb.toString();
    }
}

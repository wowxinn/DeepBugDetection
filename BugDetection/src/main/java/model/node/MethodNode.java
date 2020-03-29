package model.node;

public class MethodNode extends MyNode {

    private String label;
    private String path;
    private String classOrInterfaceDeclaration;
    private String method;

    public MethodNode() {
        this.setType("METHOD");
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getClassOrInterfaceDeclaration() {
        return classOrInterfaceDeclaration;
    }

    public void setClassOrInterfaceDeclaration(String classOrInterfaceDeclaration) {
        this.classOrInterfaceDeclaration = classOrInterfaceDeclaration;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }
}

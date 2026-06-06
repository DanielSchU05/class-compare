package vu.daniel;

public class PathNames {
    private String ontologyIDsPath;
    private String originalPath;
    private String clusteredPath;


    PathNames() {
        this.ontologyIDsPath = "";
        this.originalPath = "";
        this.clusteredPath = "";
    }

    public String getOntologyIDsPath() {
        return ontologyIDsPath;
    }

    public void setOntologyIDsPath(String ontologyIDsPath) {
        this.ontologyIDsPath = ontologyIDsPath;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public void setOriginalPath(String originalPath) {
        this.originalPath = originalPath;
    }

    public String getClusteredPath() {
        return clusteredPath;
    }

    public void setClusteredPath(String clusteredPath) {
        this.clusteredPath = clusteredPath;
    }
}


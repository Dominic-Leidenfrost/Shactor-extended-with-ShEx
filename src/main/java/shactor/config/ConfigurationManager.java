package shactor.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Configuration manager to handle loading and accessing application properties.
 * This class provides centralized access to configuration values from application.properties
 * and supports environment variable overrides.
 */
public class ConfigurationManager {
    private static ConfigurationManager instance;
    private Properties properties;
    private Map<String, String> datasetPaths;
    private Map<String, String> repositoryNames;
    
    private ConfigurationManager() {
        loadProperties();
        initializeDatasetMappings();
    }
    
    public static synchronized ConfigurationManager getInstance() {
        if (instance == null) {
            instance = new ConfigurationManager();
        }
        return instance;
    }
    
    private void loadProperties() {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                System.err.println("Unable to find application.properties");
                return;
            }
            properties.load(input);
        } catch (IOException e) {
            System.err.println("Error loading application.properties: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void initializeDatasetMappings() {
        datasetPaths = new HashMap<>();
        repositoryNames = new HashMap<>();
        
        // Initialize dataset paths
        datasetPaths.put("LUBM-Mini", getProperty("dataset.lubm.mini.path", "./datasets/lubm-mini.nt"));
        datasetPaths.put("DBpedia", getProperty("dataset.dbpedia.path", "./datasets/dbpedia_ml.nt"));
        datasetPaths.put("LUBM", getProperty("dataset.lubm.path", "./datasets/lubm.n3"));
        datasetPaths.put("YAGO-4", getProperty("dataset.yago.path", "./datasets/yago.n3"));
        
        // Initialize repository names
        repositoryNames.put("LUBM-Mini", getProperty("repository.lubm.mini", "LUBM-ScaleFactor-1"));
        repositoryNames.put("DBpedia", getProperty("repository.dbpedia", "DBPEDIA_ML"));
        repositoryNames.put("LUBM", getProperty("repository.lubm", "LUBM"));
        repositoryNames.put("YAGO-4", getProperty("repository.yago", "Yago_EngWiki"));
    }
    
    /**
     * Get a property value with support for environment variable resolution.
     * Supports ${ENV_VAR:default_value} syntax.
     */
    public String getProperty(String key, String defaultValue) {
        String value = properties.getProperty(key, defaultValue);
        return resolveEnvironmentVariables(value);
    }
    
    /**
     * Get a property value without default.
     */
    public String getProperty(String key) {
        return getProperty(key, null);
    }
    
    /**
     * Resolve environment variables in property values.
     * Supports ${ENV_VAR:default_value} syntax.
     */
    private String resolveEnvironmentVariables(String value) {
        if (value == null) return null;
        
        while (value.contains("${")) {
            int start = value.indexOf("${");
            int end = value.indexOf("}", start);
            if (end == -1) break;
            
            String envExpression = value.substring(start + 2, end);
            String envValue;
            
            if (envExpression.contains(":")) {
                String[] parts = envExpression.split(":", 2);
                String envVar = parts[0];
                String defaultVal = parts[1];
                envValue = System.getenv(envVar);
                if (envValue == null) {
                    envValue = defaultVal;
                }
            } else {
                envValue = System.getenv(envExpression);
                if (envValue == null) {
                    envValue = "";
                }
            }
            
            value = value.substring(0, start) + envValue + value.substring(end + 1);
        }
        
        return value;
    }
    
    // Dataset configuration methods
    public Map<String, String> getDatasetPaths() {
        return new HashMap<>(datasetPaths);
    }
    
    public String getDatasetPath(String datasetName) {
        return datasetPaths.get(datasetName);
    }
    
    public String getRepositoryName(String datasetName) {
        return repositoryNames.get(datasetName);
    }
    
    // SPARQL endpoint configuration methods
    public String getDefaultSparqlEndpointUrl() {
        return getProperty("sparql.endpoint.default.url", "http://localhost:7200/");
    }
    
    public String getDefaultSparqlRepository() {
        return getProperty("sparql.endpoint.default.repository", "LUBM-ScaleFactor-1");
    }
    
    public String getRemoteSparqlEndpointUrl() {
        return getProperty("sparql.endpoint.remote.url", "http://10.92.0.34:7200/");
    }
    
    /**
     * Get SPARQL endpoint URL for a specific dataset.
     * Returns default endpoint for LUBM-Mini, remote endpoint for others.
     */
    public String getSparqlEndpointUrl(String datasetName) {
        if ("LUBM-Mini".equals(datasetName)) {
            return getDefaultSparqlEndpointUrl();
        }
        return getRemoteSparqlEndpointUrl();
    }
    
    /**
     * Get complete endpoint details (URL and repository) for a dataset.
     */
    public EndpointDetails getEndpointDetails(String datasetName) {
        String url = getSparqlEndpointUrl(datasetName);
        String repository = getRepositoryName(datasetName);
        return new EndpointDetails(url, repository);
    }
    
    /**
     * Helper class to hold endpoint details.
     */
    public static class EndpointDetails {
        private final String url;
        private final String repository;
        
        public EndpointDetails(String url, String repository) {
            this.url = url;
            this.repository = repository;
        }
        
        public String getUrl() {
            return url;
        }
        
        public String getRepository() {
            return repository;
        }
    }
}
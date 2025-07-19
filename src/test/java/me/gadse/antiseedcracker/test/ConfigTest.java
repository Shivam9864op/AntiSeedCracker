package me.gadse.antiseedcracker.test;

import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Simple tests to verify configuration parsing and biome obfuscation settings.
 */
public class ConfigTest {
    
    @Test
    public void testConfigurationParsing() {
        // Load the config.yml file
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config.yml");
        assertNotNull("config.yml should exist", inputStream);
        
        // Parse the YAML
        Yaml yaml = new Yaml();
        Map<String, Object> config = yaml.load(inputStream);
        
        assertNotNull("Config should not be null", config);
        
        // Test biome_obfuscation section exists
        assertTrue("biome_obfuscation section should exist", config.containsKey("biome_obfuscation"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> biomeObfuscation = (Map<String, Object>) config.get("biome_obfuscation");
        
        // Test required settings exist
        assertTrue("enabled setting should exist", biomeObfuscation.containsKey("enabled"));
        assertTrue("login_protection_duration setting should exist", biomeObfuscation.containsKey("login_protection_duration"));
        assertTrue("respawn_protection_duration setting should exist", biomeObfuscation.containsKey("respawn_protection_duration"));
        assertTrue("log_obfuscated_chunks setting should exist", biomeObfuscation.containsKey("log_obfuscated_chunks"));
        
        // Test default values
        assertEquals("enabled should default to false", false, biomeObfuscation.get("enabled"));
        assertEquals("login_protection_duration should be 10000", 10000, biomeObfuscation.get("login_protection_duration"));
        assertEquals("respawn_protection_duration should be 5000", 5000, biomeObfuscation.get("respawn_protection_duration"));
        assertEquals("log_obfuscated_chunks should default to false", false, biomeObfuscation.get("log_obfuscated_chunks"));
    }
    
    @Test
    public void testExistingConfigIntegrity() {
        // Load the config.yml file
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config.yml");
        Yaml yaml = new Yaml();
        Map<String, Object> config = yaml.load(inputStream);
        
        // Ensure existing sections are still present
        assertTrue("randomize_hashed_seed section should exist", config.containsKey("randomize_hashed_seed"));
        assertTrue("modifiers section should exist", config.containsKey("modifiers"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> randomizeSeed = (Map<String, Object>) config.get("randomize_hashed_seed");
        assertTrue("login setting should exist in randomize_hashed_seed", randomizeSeed.containsKey("login"));
        assertTrue("respawn setting should exist in randomize_hashed_seed", randomizeSeed.containsKey("respawn"));
    }
}
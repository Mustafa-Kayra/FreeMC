package dev.freemc.game;

import dev.freemc.Os;
import dev.freemc.json.Json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** One entry of a version file's rules array: does this artefact apply to the current machine? */
final class Rule {

    private final boolean allow;
    private final String osName;
    private final String osArch;
    private final String osVersion;
    private final Map<String, Boolean> features;

    private Rule(boolean allow, String osName, String osArch, String osVersion, Map<String, Boolean> features) {
        this.allow = allow;
        this.osName = osName;
        this.osArch = osArch;
        this.osVersion = osVersion;
        this.features = features;
    }

    static List<Rule> parse(List<Object> raw) {
        List<Rule> rules = new ArrayList<>();
        for (Object element : raw) {
            if (!(element instanceof Map)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> entry = (Map<String, Object>) element;
            boolean allow = "allow".equals(Json.str(entry, "action"));
            Map<String, Object> os = Json.map(entry, "os");
            Map<String, Boolean> features = new HashMap<>();
            Map<String, Object> rawFeatures = Json.map(entry, "features");
            if (rawFeatures != null) {
                for (Map.Entry<String, Object> e : rawFeatures.entrySet()) {
                    features.put(e.getKey(), Boolean.TRUE.equals(e.getValue()));
                }
            }
            rules.add(new Rule(
                    allow,
                    Json.str(os, "name"),
                    Json.str(os, "arch"),
                    Json.str(os, "version"),
                    features));
        }
        return rules;
    }

    /**
     * Evaluates a rule list. No rules means "allowed"; otherwise the last matching rule decides.
     */
    static boolean allows(List<Rule> rules, Map<String, Boolean> features) {
        if (rules.isEmpty()) {
            return true;
        }
        boolean allowed = false;
        for (Rule rule : rules) {
            if (rule.matches(features)) {
                allowed = rule.allow;
            }
        }
        return allowed;
    }

    private boolean matches(Map<String, Boolean> features) {
        if (osName != null && !osName.equals(Os.NAME)) {
            return false;
        }
        if (osArch != null && !osArch.equals(Os.ARCH) && !(osArch.equals("x86") && Os.ARCH.equals("x86"))) {
            return false;
        }
        if (osVersion != null && !Os.VERSION.matches(osVersion)) {
            return false;
        }
        for (Map.Entry<String, Boolean> required : this.features.entrySet()) {
            boolean actual = features.getOrDefault(required.getKey(), false);
            if (actual != required.getValue()) {
                return false;
            }
        }
        return true;
    }
}

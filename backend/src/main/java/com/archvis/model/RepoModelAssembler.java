package com.archvis.model;

import com.archvis.domain.*;
import com.archvis.parser.JsonFileParser;
import com.archvis.parser.XmlFileParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Scans local repo copies and converts source files to domain objects.
 * Convention: features/**\/*.xml → Feature, datasources/**\/*.json → DataSourceDef
 * Schema-to-field mapping is defined here; detailed rules deferred to business phase (B-01).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RepoModelAssembler {

    private final XmlFileParser xmlParser;
    private final JsonFileParser jsonParser;

    public ModelSnapshot assemble(List<RepoLocalPath> repoPaths) {
        List<Feature> features = new ArrayList<>();
        List<DataSourceDef> dataSources = new ArrayList<>();

        for (RepoLocalPath entry : repoPaths) {
            if (!Files.exists(entry.path())) {
                log.warn("Repo local path not found, skipping: {}", entry.path());
                continue;
            }
            features.addAll(parseFeatures(entry));
            dataSources.addAll(parseDataSources(entry));
        }

        List<DependencyLink> links = buildLinks(features, dataSources);
        log.info("Model rebuilt: {} features, {} datasources, {} dependency links",
                features.size(), dataSources.size(), links.size());
        return ModelSnapshot.of(features, dataSources, links);
    }

    private List<Feature> parseFeatures(RepoLocalPath entry) {
        List<Feature> result = new ArrayList<>();
        Path featuresDir = entry.path().resolve("features");
        if (!Files.exists(featuresDir)) return result;

        try {
            Files.walkFileTree(featuresDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".xml")) {
                        try {
                            Feature f = xmlParser.parse(file, Feature.class);
                            f.setSourceRepo(entry.repoName());
                            result.add(f);
                            log.debug("Parsed feature: {} from {}", f.getId(), file);
                        } catch (Exception e) {
                            log.warn("Failed to parse feature file {}: {}", file, e.getMessage());
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("Error scanning features dir {}: {}", featuresDir, e.getMessage());
        }
        return result;
    }

    private List<DataSourceDef> parseDataSources(RepoLocalPath entry) {
        List<DataSourceDef> result = new ArrayList<>();
        Path dsDir = entry.path().resolve("datasources");
        if (!Files.exists(dsDir)) return result;

        try {
            Files.walkFileTree(dsDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".json")) {
                        try {
                            DataSourceDef ds = jsonParser.parse(file, DataSourceDef.class);
                            ds.setSourceRepo(entry.repoName());
                            result.add(ds);
                            log.debug("Parsed datasource: {} from {}", ds.getId(), file);
                        } catch (Exception e) {
                            log.warn("Failed to parse datasource file {}: {}", file, e.getMessage());
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("Error scanning datasources dir {}: {}", dsDir, e.getMessage());
        }
        return result;
    }

    private List<DependencyLink> buildLinks(List<Feature> features, List<DataSourceDef> dataSources) {
        List<DependencyLink> links = new ArrayList<>();
        for (Feature feature : features) {
            if (feature.getConfigs() == null) continue;
            for (FeatureConfig config : feature.getConfigs()) {
                if (config.getDataSourceRequirements() == null) continue;
                for (DataSourceRequirement req : config.getDataSourceRequirements()) {
                    links.add(new DependencyLink(
                            feature.getId(),
                            config.getId(),
                            req.getDatasourceId(),
                            req.getCollectionId(),
                            req.getMetricId()
                    ));
                }
            }
        }
        return links;
    }

    public record RepoLocalPath(String repoName, Path path) {}
}

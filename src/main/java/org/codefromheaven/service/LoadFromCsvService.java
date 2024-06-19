package org.codefromheaven.service;

import org.codefromheaven.dto.ElementType;
import org.codefromheaven.dto.FileType;
import org.codefromheaven.dto.LoadedElementDTO;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LoadFromCsvService {

    private static final String DELIMITER = ";";

    private LoadFromCsvService() { }

    public static List<LoadedElementDTO> load(FileType type) throws IOException {
        Optional<List<LoadedElementDTO>> commands = loadBase(type.getPersonalizedConfigName());
        return commands.orElseGet(() -> loadBase(type.getDefaultFileName()).get());
    }

    public static Optional<List<LoadedElementDTO>> loadBase(String configPath) {
        List<LoadedElementDTO> commands = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(configPath))) {
            String line;
            // skip first line
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] values = line.split(DELIMITER);
                commands.add(new LoadedElementDTO(
                        values[2], values[3], values[4], values[5],
                        values[6], ElementType.getEnumType(values[7]), Boolean.parseBoolean(values[8]),
                        Boolean.parseBoolean(values[9]), values[10], values[11]));
            }
        } catch (IOException e) {
            return Optional.empty();
        }
        return commands.isEmpty() ? Optional.empty() : Optional.of(commands);
    }

}
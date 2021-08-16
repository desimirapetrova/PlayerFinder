package com.example.football.service.impl;

import com.example.football.models.dto.TeamSeedDto;
import com.example.football.models.entity.Team;
import com.example.football.models.entity.Town;
import com.example.football.repository.TeamRepository;
import com.example.football.service.TeamService;
import com.example.football.service.TownService;
import com.example.football.util.ValidationUtil;
import com.google.gson.Gson;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

//ToDo - Implement all methods
@Service
public class TeamServiceImpl implements TeamService {
    public static final String TEAM_FILE_PATH="src/main/resources/files/json/teams.json";
    private final TeamRepository teamRepository;
    private final TownService townService;
    private final Gson gson;
    private final ModelMapper modelMapper;
    private final ValidationUtil validationUtil;

    public TeamServiceImpl(TeamRepository teamRepository, TownService townService, Gson gson, ModelMapper modelMapper, ValidationUtil validationUtil) {
        this.teamRepository = teamRepository;
        this.townService = townService;
        this.gson = gson;
        this.modelMapper = modelMapper;
        this.validationUtil = validationUtil;
    }

    @Override
    public boolean areImported() {
        return teamRepository.count()>0;
    }

    @Override
    public String readTeamsFileContent() throws IOException {
        return Files.readString(Path.of(TEAM_FILE_PATH));
    }

    @Override
    public String importTeams() throws IOException {
        StringBuilder sb=new StringBuilder();
        Arrays.stream(gson
                .fromJson(readTeamsFileContent(), TeamSeedDto[].class))
                .filter(teamSeedDto -> {
                    boolean isValid=validationUtil.isValid(teamSeedDto);
                    sb.append(isValid?String.format("Successfully imported Team %s - %d",
                            teamSeedDto.getName(),teamSeedDto.getFanBase())
                            :"Invalid team")
                            .append(System.lineSeparator());
                    return isValid;
                }).map(teamSeedDto -> {
           Team team= modelMapper.map(teamSeedDto, Team.class);
          //  Town town=townService.getTownByName(teamSeedDto.getTownName().getName());
          //  team.setTown(town);
            team.setTown(townService.findByName(teamSeedDto.getTownName()));
            return team;
          })
                .forEach(teamRepository::save);
        return sb.toString();
    }


    @Override
    public Team getTeamByName(String name) {
        return teamRepository.findTeamByName(name);
    }
}

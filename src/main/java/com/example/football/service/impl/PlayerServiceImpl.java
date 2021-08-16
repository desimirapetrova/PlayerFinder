package com.example.football.service.impl;

import com.example.football.models.dto.PlayerSeedRootDto;
import com.example.football.models.entity.Player;
import com.example.football.models.entity.Stat;
import com.example.football.models.entity.Team;
import com.example.football.models.entity.Town;
import com.example.football.repository.PlayerRepository;
import com.example.football.service.PlayerService;
import com.example.football.service.StatService;
import com.example.football.service.TeamService;
import com.example.football.service.TownService;
import com.example.football.util.ValidationUtil;
import com.example.football.util.XmlParser;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class PlayerServiceImpl implements PlayerService {

    public static final String PLAYER_FILE_PATH="src/main/resources/files/xml/players.xml";

    private final TownService townService;
    private final TeamService teamService;
    private final StatService statService;
    private final PlayerRepository playerRepository;
    private final ValidationUtil validationUtil;
    private final ModelMapper modelMapper;
    private final XmlParser xmlParser;

    public PlayerServiceImpl(TownService townService, TeamService teamService, StatService statService, PlayerRepository playerRepository, ValidationUtil validationUtil, ModelMapper modelMapper, XmlParser xmlParser) {
        this.townService = townService;
        this.teamService = teamService;
        this.statService = statService;

        this.playerRepository = playerRepository;
        this.validationUtil = validationUtil;
        this.modelMapper = modelMapper;
        this.xmlParser = xmlParser;
    }

    @Override
    public boolean areImported() {
        return playerRepository.count()>0;
    }

    @Override
    public String readPlayersFileContent() throws IOException {
        return Files.readString(Path.of(PLAYER_FILE_PATH));
    }

    @Override
    public String importPlayers() throws JAXBException, FileNotFoundException {
        StringBuilder sb=new StringBuilder();
        xmlParser.fromFile(PLAYER_FILE_PATH, PlayerSeedRootDto.class)
                .getPlayers()
                .stream()
                .filter(playerSeedDto -> {
                    boolean isValid = validationUtil.isValid(playerSeedDto);
                    sb.append(isValid ? String.format("Successfully imported Player %s %s - %s",
                            playerSeedDto.getFirstName(),
                            playerSeedDto.getLastName(),
                            playerSeedDto.getPosition())
                            : "Invalid Player")
                            .append(System.lineSeparator());
                    return isValid;
                })
                .map(playerSeedDto -> {
                    Player player=modelMapper.map(playerSeedDto,Player.class);
                    Town town=townService.getTownByName(playerSeedDto.getTown().getName());
                    Team team=teamService.getTeamByName(playerSeedDto.getTeam().getName());
                    Stat stat=statService.getStatById(playerSeedDto.getStat().getId());
                    //  player.setTown(townService.getTownByName(playerSeedDto.getTown().getName()));
                //    player.setTeam(teamService.findByName(playerSeedDto.getTeam().getName());
                   // player.setStat(statService.findByid(playerSeedDto.getStat().getId()));
                   player.setTown(town);
                   player.setTeam(team);
                   player.setStat(stat);
                    return player;
                }).forEach(playerRepository::save);

        return sb.toString();
    }

    @Override
    public String exportBestPlayers() {
        StringBuilder sb=new StringBuilder();
        playerRepository.findBestPlayer()
                .stream()
                .forEach(player -> {
                    sb.append(String.format("Player - %s %s\n" +
                            "\tPosition - %s\n" +
                            "\tTeam - %s\n" +
                            "\tStadium - %s",
                            player.getFirstName(),
                            player.getLastName(),
                            player.getPosition(),
                            player.getTeam().getName(),
                            player.getTeam().getStadiumName()))
                            .append(System.lineSeparator());
                });
        return sb.toString();
    }
}

package htn.bfdiscordintegration;

import htn.bfdiscordintegration.models.ChatModel;
import htn.bfdiscordintegration.models.PlayerModel;
import htn.bfdiscordintegration.models.PlayerStatModel;
import htn.bfdiscordintegration.models.RoundStatModel;
import htn.bfdiscordintegration.models.enums.TeamEnum;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.util.StringUtils;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class EventlogMapper {

    private static final Logger log = LogManager.getLogger(EventlogMapper.class);

    private final Map<Integer, PlayerModel> knownPlayers = new HashMap<>();

    private long unixTimestampBegin = 0l;

    private final Pattern BEGINTIMESTAMPPATTERN = Pattern.compile("^.*timestamp=\"(\\d*_\\d*)\".*$");

    private DocumentBuilder documentBuilder;

    public void reset() {
        log.info("Reset EventlogMapper");
        knownPlayers.clear();
        unixTimestampBegin = 0l;
    }

    /**
     * @param inputLine e.g.
     * <bf:log engine="BFVietnam v1.21" timestamp="20220814_1830" xmlns:bf="http://www.dice.se/xmlns/bf/1.0">
     */
    public void handleBeginTimestamp(String inputLine) {
        log.debug("Set BeginTimestamp " + inputLine);
        // Now create matcher object.
        Matcher m = BEGINTIMESTAMPPATTERN.matcher(inputLine);

        if (m.find() && m.groupCount() == 1) {
            String timestamp = m.group(1);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmm");
            unixTimestampBegin = 0l;
            try {
                Date dt = sdf.parse(timestamp);
                unixTimestampBegin = dt.getTime();
                log.debug("Set BeginUnixTimestamp " + unixTimestampBegin);
            } catch (ParseException e) {
                log.warn("Error parsing timestamp of timestampStr with format yyyyMMdd_HHmm! Using 0-based timestamp", e);
            }
        }
    }

    public Optional<RoundStatModel> handleRoundStats(String roundstats) {
        log.info("Handle roundstats " + roundstats);
        for (Map.Entry<Integer, String> mapEntry : ISO8859Character.characterMap.entrySet()) {
            roundstats = roundstats.replaceAll("(<bf:nonprint>" + mapEntry.getKey() + "</bf:nonprint>)", mapEntry.getValue());
        }
        Document doc;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            this.documentBuilder = factory.newDocumentBuilder();
            ByteArrayInputStream input = new ByteArrayInputStream(roundstats.getBytes("ISO-8859-1"));
            Reader reader = new InputStreamReader(input, "ISO-8859-1");
            InputSource is = new InputSource(reader);
            is.setEncoding("ISO-8859-1");
            doc = documentBuilder.parse(is);

            RoundStatModel roundStatModel = new RoundStatModel();

            Element docEl = doc.getDocumentElement();

            roundStatModel.setWinningTeam(TeamEnum.findByCode(Integer.parseInt(((Element) docEl.getElementsByTagName("bf:winningteam")).getNodeValue())));

            NodeList subNodeTeamTickets = (NodeList) docEl.getElementsByTagName("bf:teamtickets");
            for (int i = 0; i < subNodeTeamTickets.getLength(); i++) {
                Element subNode = (Element) subNodeTeamTickets.item(i);
                switch (subNode.getAttribute("team")) {
                    case "1":
                        roundStatModel.setRedTickets(Integer.parseInt(subNode.getFirstChild().getNodeValue()));
                        break;
                    case "2":
                        roundStatModel.setBlueTickets(Integer.parseInt(subNode.getFirstChild().getNodeValue()));
                        break;
                }
            }

            NodeList subNodeListStats = (NodeList) docEl.getElementsByTagName("bf:playerstat");
            for (int j = 0; j < subNodeListStats.getLength(); j++) {
                Element subNode = (Element) subNodeListStats.item(j);
                NodeList subSubNodeListStats = (NodeList) subNode.getElementsByTagName("bf:statparam");
                for (int k = 0; k < subSubNodeListStats.getLength(); k++) {
                    Element subSubNode = (Element) subSubNodeListStats.item(k);
                    PlayerStatModel playerStatModel = new PlayerStatModel();
                    switch (subSubNode.getAttribute("name")) {
                        case "player_name":
                            playerStatModel.setPlayerName(subSubNode.getFirstChild().getNodeValue());
                            break;
                        case "team":
                            playerStatModel.setTeam(TeamEnum.findByCode(Integer.parseInt(subSubNode.getFirstChild().getNodeValue())));
                            break;
                        case "score":
                            playerStatModel.setScore(Integer.parseInt(subSubNode.getFirstChild().getNodeValue()));
                            break;
                        case "kills":
                            playerStatModel.setKills(Integer.parseInt(subSubNode.getFirstChild().getNodeValue()));
                            break;
                        case "deaths":
                            playerStatModel.setDeaths(Integer.parseInt(subSubNode.getFirstChild().getNodeValue()));
                            break;
                    }
                    roundStatModel.getPlayerModels().add(playerStatModel);
                }
            }
            return Optional.of(roundStatModel);
        } catch (IOException | NumberFormatException | BeansException | DOMException | SAXException | ParserConfigurationException ex) {
            log.warn("Error handling node " + roundstats, ex);
        }

        return Optional.empty();
    }

    /**
     *
     * @param event e.g.
     * <bf:event name="createVehicle" timestamp="5.18721">...</bf:event>
     * @return ChatModel if a chat is recognized for further handling, or empty
     * Optional if just metadata received
     */
    public Optional<ChatModel> handleBfEvent(String event) {
        log.trace("Handle event " + event);
        Document doc;
        for (Map.Entry<Integer, String> mapEntry : ISO8859Character.characterMap.entrySet()) {
            event = event.replaceAll("(<bf:nonprint>" + mapEntry.getKey() + "</bf:nonprint>)", mapEntry.getValue());
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            this.documentBuilder = factory.newDocumentBuilder();
            ByteArrayInputStream input = new ByteArrayInputStream(event.getBytes("ISO-8859-1"));
            Reader reader = new InputStreamReader(input, "ISO-8859-1");
            InputSource is = new InputSource(reader);
            is.setEncoding("ISO-8859-1");
            doc = documentBuilder.parse(is);
            Element docEl = doc.getDocumentElement();
            switch (docEl.getAttribute("name")) {
                case "createPlayer":
                    PlayerModel playerModel = new PlayerModel();

                    NodeList subNodeListPlayer = (NodeList) docEl.getElementsByTagName("bf:param");
                    for (int j = 0; j < subNodeListPlayer.getLength(); j++) {
                        Element subNode = (Element) subNodeListPlayer.item(j);
                        switch (subNode.getAttribute("name")) {
                            case "name":
                                playerModel.setName(subNode.getFirstChild().getNodeValue());
                                playerModel.setCurrentName(subNode.getFirstChild().getNodeValue());
                                break;
                            case "player_id":
                                playerModel.setPlayerId(Integer.valueOf(subNode.getFirstChild().getNodeValue()));
                                break;
                            case "is_ai":
                                playerModel.setIsAi(subNode.getFirstChild().getNodeValue().equals("1"));
                                break;
                            case "team":
                                playerModel.setTeam(Integer.valueOf(subNode.getFirstChild().getNodeValue()));
                                break;
                        }
                    }
                    knownPlayers.put(playerModel.getPlayerId(), playerModel);
                    break;
                case "changePlayerName":

                    NodeList subNodeListPlayerNameChange = (NodeList) docEl.getElementsByTagName("bf:param");
                    int player_id = -1;
                    String new_name = "";
                    for (int j = 0; j < subNodeListPlayerNameChange.getLength(); j++) {
                        Element subNode = (Element) subNodeListPlayerNameChange.item(j);
                        switch (subNode.getAttribute("name")) {
                            case "name":
                                new_name = subNode.getFirstChild().getNodeValue();
                                break;
                            case "player_id":
                                player_id = Integer.valueOf(subNode.getFirstChild().getNodeValue());
                                break;
                        }
                    }
                    if (player_id >= 0) {
                        if (knownPlayers.containsKey(player_id) && StringUtils.hasText(new_name)) {
                            PlayerModel newModel = new PlayerModel();
                            BeanUtils.copyProperties(knownPlayers.get(player_id), newModel);
                            newModel.setName(new_name + " (" + newModel.getName() + ")");
                            newModel.setCurrentName(new_name);
                            knownPlayers.put(player_id, newModel);
                        }
                    }
                    break;
                case "chat":
                    String elementTimestampStr = docEl.getAttribute("timestamp");
                    long elementTimestampMillis = (long) (Double.valueOf(elementTimestampStr) * 1000 + unixTimestampBegin);

                    SimpleDateFormat outputDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                    String dateOutput = outputDateFormat.format(new Date(elementTimestampMillis));

                    ChatModel chatModel = new ChatModel();
                    chatModel.setFormattedTimestamp(dateOutput);

                    NodeList subNodeListChat = (NodeList) docEl.getElementsByTagName("bf:param");
                    for (int j = 0; j < subNodeListChat.getLength(); j++) {
                        Element subNode = (Element) subNodeListChat.item(j);
                        switch (subNode.getAttribute("name")) {
                            case "player_id":
                                int playerId = Integer.parseInt(subNode.getFirstChild().getNodeValue());
                                if (knownPlayers.containsKey(playerId)) {
                                    chatModel.setPlayerModel(knownPlayers.get(playerId));
                                }
                                break;
                            case "team":
                                chatModel.setTeam(Integer.valueOf(subNode.getFirstChild().getNodeValue()));
                                break;
                            case "text":
                                chatModel.setText(subNode.getFirstChild().getNodeValue().replace("&lt;", "<").replace("&gt;", ">"));
                                break;
                        }
                    }
                    return Optional.of(chatModel);
                default:
                    log.trace("No handled attribute " + docEl.getAttribute("name"));
                    break;
            }
        } catch (IOException | NumberFormatException | BeansException | DOMException | SAXException | ParserConfigurationException ex) {
            log.warn("Error handling node " + event, ex);
        }
        return Optional.empty();
    }
}

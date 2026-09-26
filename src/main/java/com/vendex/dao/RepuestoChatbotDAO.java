package com.vendex.dao;

import com.vendex.chatbot.GeminiChatbotService;
import com.vendex.config.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface RepuestoChatbotDAO extends GeminiChatbotService.RepuestoDao {

}

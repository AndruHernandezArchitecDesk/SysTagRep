package com.vendex.remote;
import com.vendex.chatbot.GeminiChatbotService;
import com.vendex.dao.RepuestoChatbotDAO;
import java.util.Map;
public class RepuestoChatbotDAORest implements RepuestoChatbotDAO {
    private final RestClient rest;
    public RepuestoChatbotDAORest(RestClient rest) { this.rest = rest; }
    public GeminiChatbotService.ResultadoBusqueda buscar(String descripcion, String marca, String modelo, Integer anio) { try { return rest.get("/repuesto-chatbot/buscar?descripcion="+descripcion+"&marca="+marca+"&modelo="+modelo+"&anio="+anio, GeminiChatbotService.ResultadoBusqueda.class); } catch (Exception e) { return null; } }
}

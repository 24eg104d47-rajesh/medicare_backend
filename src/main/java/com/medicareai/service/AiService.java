package com.medicareai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medicareai.dto.ai.*;
import com.medicareai.dto.medicine.MedicineResponse;
import com.medicareai.entity.AiConversation;
import com.medicareai.entity.AiMessage;
import com.medicareai.entity.Medicine;
import com.medicareai.entity.User;
import com.medicareai.enums.AiSender;
import com.medicareai.exception.ResourceNotFoundException;
import com.medicareai.repository.AiConversationRepository;
import com.medicareai.repository.AiMessageRepository;
import com.medicareai.repository.MedicineRepository;
import com.medicareai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiService {

    private static final Logger logger = LoggerFactory.getLogger(AiService.class);

    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;
    private final MedicineRepository medicineRepository;
    private final MedicineService medicineService;
    private final UserRepository userRepository;

    @Value("${app.ai.api-key:}")
    private String aiApiKey;

    @Value("${app.ai.api-url:https://api.openai.com/v1/chat/completions}")
    private String aiApiUrl;

    @Value("${app.ai.model:gpt-4o-mini}")
    private String aiModel;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String STANDARD_DISCLAIMER =
            "Medical Disclaimer: MediCare AI is an informational pharmacy assistant. It does not provide medical diagnosis, prescribe drugs, or substitute for a consultation with a registered doctor or licensed pharmacist. For urgent symptoms, seek emergency care.";

    @Transactional
    public AiChatResponse chat(Long userId, AiChatRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Get or create conversation
        AiConversation conversation;
        if (request.getConversationId() != null) {
            conversation = conversationRepository.findByIdAndUserId(request.getConversationId(), userId)
                    .orElseGet(() -> createNewConversation(user, request.getMessage()));
        } else {
            conversation = createNewConversation(user, request.getMessage());
        }

        // Save User Message
        AiMessage userMsg = AiMessage.builder()
                .conversation(conversation)
                .sender(AiSender.USER)
                .message(request.getMessage().trim())
                .build();
        messageRepository.save(userMsg);

        // Analyze Intent & Catalogue Query
        String userQuery = request.getMessage().toLowerCase();
        String intent = detectIntent(userQuery);

        List<MedicineResponse> matchedMedicines = searchRelevantCatalogue(userQuery);
        String aiResponseText = generateAssistantReply(request.getMessage(), intent, matchedMedicines);

        // Save AI Message
        AiMessage aiMsg = AiMessage.builder()
                .conversation(conversation)
                .sender(AiSender.AI)
                .message(aiResponseText)
                .build();
        messageRepository.save(aiMsg);

        return AiChatResponse.builder()
                .conversationId(conversation.getId())
                .message(aiResponseText)
                .intent(intent)
                .recommendedMedicines(matchedMedicines)
                .disclaimer(STANDARD_DISCLAIMER)
                .build();
    }

    @Transactional(readOnly = true)
    public AiMedicineInfoResponse getMedicineInformation(AiMedicineInfoRequest request) {
        Medicine medicine = null;
        if (request.getMedicineId() != null) {
            medicine = medicineRepository.findById(request.getMedicineId()).orElse(null);
        } else if (request.getQuery() != null && !request.getQuery().isBlank()) {
            List<Medicine> found = medicineRepository.searchActiveMedicines(request.getQuery().trim(), PageRequest.of(0, 1)).getContent();
            if (!found.isEmpty()) {
                medicine = found.get(0);
            }
        }

        if (medicine == null) {
            return AiMedicineInfoResponse.builder()
                    .answer("I could not find a verified medicine matching '" + request.getQuery() +
                            "' in our pharmacy database. Please verify the medicine name or speak to a pharmacist.")
                    .medicineDetails(null)
                    .safetyDisclaimer(STANDARD_DISCLAIMER)
                    .build();
        }

        MedicineResponse enriched = medicineService.enrichAndMapToResponse(medicine);
        StringBuilder sb = new StringBuilder();
        sb.append("**").append(enriched.getName()).append("** (Brand: ").append(enriched.getBrand()).append(")\n\n");
        sb.append("- **Generic Formula:** ").append(enriched.getGenericName()).append("\n");
        sb.append("- **Dosage Form & Strength:** ").append(enriched.getDosageForm()).append(" ").append(enriched.getStrength()).append("\n");
        sb.append("- **Pack Size:** ").append(enriched.getPackSize()).append("\n");
        sb.append("- **Manufacturer:** ").append(enriched.getManufacturer()).append("\n");
        sb.append("- **Prescription Required:** ").append(enriched.isRequiresPrescription() ? "Yes (Valid doctor prescription required at checkout)" : "No (Over The Counter)").append("\n");
        sb.append("- **Current Stock Status:** ").append(enriched.getAvailableStock() > 0 ? "In Stock (" + enriched.getAvailableStock() + " units available at ₹" + enriched.getPrice() + ")" : "Currently Out of Stock").append("\n\n");
        sb.append(enriched.getDescription() != null ? enriched.getDescription() : "Verified catalogue pharmaceutical product.");

        return AiMedicineInfoResponse.builder()
                .answer(sb.toString())
                .medicineDetails(enriched)
                .safetyDisclaimer(STANDARD_DISCLAIMER)
                .build();
    }

    @Transactional(readOnly = true)
    public AiProductDiscoveryResponse discoverProducts(AiProductDiscoveryRequest request) {
        String query = request.getDescription().trim();
        List<MedicineResponse> matches = searchRelevantCatalogue(query.toLowerCase());

        String advice;
        if (matches.isEmpty()) {
            advice = "We could not find matching products for \"" + query + "\" in our current pharmacy catalogue. " +
                     "Please consult our registered pharmacist or explore our category list.";
        } else {
            advice = "Found " + matches.size() + " verified product(s) in our pharmacy inventory matching \"" + query + "\":";
        }

        return AiProductDiscoveryResponse.builder()
                .advice(advice)
                .matchedProducts(matches)
                .disclaimer(STANDARD_DISCLAIMER)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AiConversationResponse> getConversationHistory(Long userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(this::mapConversationToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AiConversationResponse getConversationById(Long userId, Long conversationId) {
        AiConversation conv = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
        return mapConversationToResponse(conv);
    }

    @Transactional
    public void deleteConversation(Long userId, Long conversationId) {
        AiConversation conv = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
        conversationRepository.delete(conv);
    }

    private AiConversation createNewConversation(User user, String firstMsg) {
        String title = firstMsg.length() > 40 ? firstMsg.substring(0, 37) + "..." : firstMsg;
        AiConversation conv = AiConversation.builder()
                .user(user)
                .title(title)
                .build();
        return conversationRepository.save(conv);
    }

    private String detectIntent(String query) {
        if (query.contains("cart") || query.contains("basket")) return "VIEW_CART";
        if (query.contains("order") || query.contains("track") || query.contains("status")) return "EXPLAIN_ORDER_STATUS";
        if (query.contains("stock") || query.contains("available") || query.contains("inventory")) return "CHECK_STOCK";
        if (query.contains("category") || query.contains("department") || query.contains("types")) return "FIND_CATEGORY";
        if (query.contains("looking for") || query.contains("need") || query.contains("search") || query.contains("find") || query.contains("buy")) return "SEARCH_PRODUCT";
        return "GENERAL_STORE_QUESTION";
    }

    private List<MedicineResponse> searchRelevantCatalogue(String query) {
        // Remove common filler words
        String cleanQuery = query.replaceAll("(?i)\\b(i|am|looking|for|need|a|an|the|to|buy|can|you|find|me|show)\\b", "").trim();
        if (cleanQuery.isBlank()) {
            cleanQuery = query;
        }

        List<Medicine> medicines = medicineRepository.searchActiveMedicines(cleanQuery, PageRequest.of(0, 6)).getContent();
        return medicines.stream()
                .map(medicineService::enrichAndMapToResponse)
                .collect(Collectors.toList());
    }

    private String generateAssistantReply(String userMessage, String intent, List<MedicineResponse> medicines) {
        // If external AI key is configured, attempt call with strict guardrails
        if (aiApiKey != null && !aiApiKey.isBlank() && !aiApiKey.startsWith("your_")) {
            try {
                String externalReply = callExternalAi(userMessage, medicines);
                if (externalReply != null && !externalReply.isBlank()) {
                    return externalReply;
                }
            } catch (Exception e) {
                logger.warn("External AI call failed, falling back to local grounded engine: {}", e.getMessage());
            }
        }

        // Grounded Local AI Pharmacy Engine
        StringBuilder sb = new StringBuilder();
        switch (intent) {
            case "SEARCH_PRODUCT", "CHECK_STOCK" -> {
                if (!medicines.isEmpty()) {
                    sb.append("Here are verified medicines matching your request in our pharmacy inventory:\n\n");
                    for (MedicineResponse m : medicines) {
                        sb.append("• **").append(m.getName()).append("** (").append(m.getStrength()).append(")\n");
                        sb.append("  - Brand: ").append(m.getBrand()).append(" | ").append(m.getDosageForm()).append("\n");
                        sb.append("  - Price: ₹").append(m.getPrice()).append(" | Status: ")
                                .append(m.getAvailableStock() > 0 ? "In Stock (" + m.getAvailableStock() + " available)" : "Out of Stock").append("\n");
                        if (m.isRequiresPrescription()) {
                            sb.append("  - ⚠️ Doctor Prescription Required\n");
                        }
                    }
                } else {
                    sb.append("I checked our pharmacy database, but couldn't find a direct match. You can search by brand, active salt, or browse our categories.");
                }
            }
            case "VIEW_CART" -> sb.append("You can view and modify items in your shopping cart by opening the Cart drawer in the navigation bar.");
            case "EXPLAIN_ORDER_STATUS" -> sb.append("Orders move through these transparent steps: Placed → Confirmed (after payment) → Processing → Packed → Shipped → Out for Delivery → Delivered. You can track real-time progress on your Orders page.");
            case "FIND_CATEGORY" -> sb.append("We offer categories including Pain Relief, Antibiotics, Cardiovascular, Diabetes Care, Respiratory, Gastrointestinal, Vitamins & Supplements, and First Aid.");
            default -> {
                sb.append("Hello! I am your MediCare AI pharmacy assistant. I can help you search our catalogue, check medicine ingredients, verify stock availability, and check prescription requirements.\n\n");
                if (!medicines.isEmpty()) {
                    sb.append("Regarding your query, here are relevant items available in our store:\n");
                    for (MedicineResponse m : medicines) {
                        sb.append("• ").append(m.getName()).append(" (₹").append(m.getPrice()).append(")\n");
                    }
                }
            }
        }

        return sb.toString();
    }

    private String callExternalAi(String userMessage, List<MedicineResponse> medicines) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiApiKey);

        StringBuilder catalogContext = new StringBuilder("Available Pharmacy Catalogue Matches:\n");
        for (MedicineResponse m : medicines) {
            catalogContext.append(String.format("- %s by %s, Generic: %s, Price: ₹%s, In Stock: %d, Requires Prescription: %b\n",
                    m.getName(), m.getBrand(), m.getGenericName(), m.getPrice(), m.getAvailableStock(), m.isRequiresPrescription()));
        }

        String systemPrompt = "You are MediCare AI, a professional pharmacy assistant. Ground your answers strictly in the provided catalogue inventory. Do not prescribe medicines or invent therapeutic facts. Do not provide medical diagnosis. Direct users to licensed healthcare professionals for clinical symptoms.\n" + catalogContext;

        Map<String, Object> body = new HashMap<>();
        body.put("model", aiModel);
        body.put("messages", Arrays.asList(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
        ));
        body.put("max_tokens", 400);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(aiApiUrl, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            try {
                JsonNode root = objectMapper.readTree(response.getBody());
                return root.path("choices").path(0).path("message").path("content").asText();
            } catch (Exception e) {
                logger.error("Error parsing external AI response: {}", e.getMessage());
            }
        }
        return null;
    }

    private AiConversationResponse mapConversationToResponse(AiConversation c) {
        List<AiMessageResponse> msgResponses = messageRepository.findByConversationIdOrderByCreatedAtAsc(c.getId()).stream()
                .map(m -> AiMessageResponse.builder()
                        .id(m.getId())
                        .sender(m.getSender())
                        .message(m.getMessage())
                        .createdAt(m.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return AiConversationResponse.builder()
                .id(c.getId())
                .userId(c.getUser().getId())
                .title(c.getTitle())
                .messages(msgResponses)
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}

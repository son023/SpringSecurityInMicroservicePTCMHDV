package com.example.ftgokitchenservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class KitchenService {
    private static final Logger log = LoggerFactory.getLogger(KitchenService.class);

    private final KitchenOrderRepository kitchenOrderRepository;
    private final TicketRepository ticketRepository;

    @Autowired
    public KitchenService(KitchenOrderRepository kitchenOrderRepository, TicketRepository ticketRepository) {
        this.kitchenOrderRepository = kitchenOrderRepository;
        this.ticketRepository = ticketRepository;
    }

    /**
     * Lấy tất cả đơn nhà bếp
     */
    public List<KitchenOrderDTO> getAllKitchenOrders() {
        log.info("Lấy tất cả đơn nhà bếp");
        return kitchenOrderRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lấy đơn nhà bếp theo ID
     */
    public KitchenOrderDTO getKitchenOrderById(Long id) {
        log.info("Lấy đơn nhà bếp theo ID: {}", id);
        KitchenOrder kitchenOrder = kitchenOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn nhà bếp với ID: " + id));
        return convertToDTO(kitchenOrder);
    }

    /**
     * Lấy đơn nhà bếp theo order ID
     */
    public KitchenOrderDTO getKitchenOrderByOrderId(Long orderId) {
        log.info("Lấy đơn nhà bếp theo order ID: {}", orderId);
        KitchenOrder kitchenOrder = kitchenOrderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn nhà bếp với order ID: " + orderId));
        return convertToDTO(kitchenOrder);
    }

    /**
     * Tạo đơn nhà bếp mới
     */
    @Transactional
    public KitchenOrderDTO createKitchenOrder(KitchenOrderDTO kitchenOrderDTO) {
        log.info("Tạo đơn nhà bếp mới cho order ID: {}", kitchenOrderDTO.getOrderId());

        // Kiểm tra xem đã tồn tại đơn nhà bếp với order ID này chưa
        if (kitchenOrderRepository.findByOrderId(kitchenOrderDTO.getOrderId()).isPresent()) {
            throw new RuntimeException("Đã tồn tại đơn nhà bếp với order ID: " + kitchenOrderDTO.getOrderId());
        }

        // Tạo đơn nhà bếp mới
        KitchenOrder kitchenOrder = new KitchenOrder();
        kitchenOrder.setOrderId(kitchenOrderDTO.getOrderId());
        kitchenOrder.setStatus("RECEIVED");
        kitchenOrder.setCreatedAt(LocalDateTime.now());

        KitchenOrder savedKitchenOrder = kitchenOrderRepository.save(kitchenOrder);
        log.info("Đã tạo đơn nhà bếp với ID: {}", savedKitchenOrder.getId());

        // Tạo các ticket cho đơn nhà bếp
        if (kitchenOrderDTO.getTickets() != null && !kitchenOrderDTO.getTickets().isEmpty()) {
            List<Ticket> tickets = kitchenOrderDTO.getTickets().stream()
                    .map(ticketDTO -> {
                        Ticket ticket = new Ticket();
                        ticket.setKitchenOrder(savedKitchenOrder);
                        ticket.setItemName(ticketDTO.getItemName());
                        ticket.setNotes(ticketDTO.getNotes());
                        ticket.setStatus("WAITING");
                        ticket.setCreatedAt(LocalDateTime.now());
                        return ticket;
                    })
                    .collect(Collectors.toList());

            ticketRepository.saveAll(tickets);
            log.info("Đã tạo {} ticket cho đơn nhà bếp ID: {}", tickets.size(), savedKitchenOrder.getId());
        }

        return getKitchenOrderById(savedKitchenOrder.getId());
    }

    /**
     * Cập nhật trạng thái đơn nhà bếp
     */
    @Transactional
    public KitchenOrderDTO updateKitchenOrderStatus(Long orderId, KitchenOrderStatusDTO statusDTO) {
        log.info("Cập nhật trạng thái đơn nhà bếp với order ID: {} thành {}", orderId, statusDTO.getStatus());

        // Kiểm tra quyền truy cập
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isChef = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CHEF"));

        if (!isAdmin && !isChef) {
            throw new AccessDeniedException("Bạn không có quyền cập nhật trạng thái đơn nhà bếp");
        }

        KitchenOrder kitchenOrder = kitchenOrderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn nhà bếp với order ID: " + orderId));

        kitchenOrder.setStatus(statusDTO.getStatus());

        // Nếu trạng thái là PREPARED, cập nhật thời gian hoàn thành
        if ("PREPARED".equals(statusDTO.getStatus())) {
            kitchenOrder.setPreparedAt(LocalDateTime.now());

            // Nếu là đầu bếp, cập nhật thông tin đầu bếp
            if (isChef) {
                Long chefId = Long.parseLong(authentication.getName());
                String chefName = authentication.getName(); // Trong thực tế, có thể lấy tên từ JWT claims

                kitchenOrder.setChefId(chefId);
                kitchenOrder.setChefName(chefName);
            }
        }

        KitchenOrder updatedKitchenOrder = kitchenOrderRepository.save(kitchenOrder);
        log.info("Đã cập nhật trạng thái đơn nhà bếp ID: {} thành {}", updatedKitchenOrder.getId(), updatedKitchenOrder.getStatus());

        return convertToDTO(updatedKitchenOrder);
    }

    /**
     * Cập nhật trạng thái ticket
     */
    @Transactional
    public TicketDTO updateTicketStatus(Long ticketId, String status) {
        log.info("Cập nhật trạng thái ticket ID: {} thành {}", ticketId, status);

        // Kiểm tra quyền truy cập
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isChef = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CHEF"));

        if (!isAdmin && !isChef) {
            throw new AccessDeniedException("Bạn không có quyền cập nhật trạng thái ticket");
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ticket với ID: " + ticketId));

        ticket.setStatus(status);

        // Nếu trạng thái là COMPLETED, cập nhật thời gian hoàn thành
        if ("COMPLETED".equals(status)) {
            ticket.setCompletedAt(LocalDateTime.now());
        }

        Ticket updatedTicket = ticketRepository.save(ticket);
        log.info("Đã cập nhật trạng thái ticket ID: {} thành {}", updatedTicket.getId(), updatedTicket.getStatus());

        // Kiểm tra nếu tất cả ticket đã hoàn thành thì cập nhật trạng thái đơn nhà bếp
        List<Ticket> tickets = ticketRepository.findByKitchenOrderId(ticket.getKitchenOrder().getId());
        boolean allCompleted = tickets.stream().allMatch(t -> "COMPLETED".equals(t.getStatus()));

        if (allCompleted) {
            KitchenOrder kitchenOrder = ticket.getKitchenOrder();
            kitchenOrder.setStatus("PREPARED");
            kitchenOrder.setPreparedAt(LocalDateTime.now());

            // Nếu là đầu bếp, cập nhật thông tin đầu bếp
            if (isChef) {
                Long chefId = Long.parseLong(authentication.getName());
                String chefName = authentication.getName(); // Trong thực tế, có thể lấy tên từ JWT claims

                kitchenOrder.setChefId(chefId);
                kitchenOrder.setChefName(chefName);
            }

            kitchenOrderRepository.save(kitchenOrder);
            log.info("Tất cả ticket đã hoàn thành, cập nhật trạng thái đơn nhà bếp ID: {} thành PREPARED", kitchenOrder.getId());
        }

        return convertToDTO(updatedTicket);
    }

    /**
     * Chuyển đổi KitchenOrder thành DTO
     */
    private KitchenOrderDTO convertToDTO(KitchenOrder kitchenOrder) {
        List<Ticket> tickets = ticketRepository.findByKitchenOrderId(kitchenOrder.getId());

        List<TicketDTO> ticketDTOs = tickets.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return KitchenOrderDTO.builder()
                .id(kitchenOrder.getId())
                .orderId(kitchenOrder.getOrderId())
                .status(kitchenOrder.getStatus())
                .createdAt(kitchenOrder.getCreatedAt())
                .preparedAt(kitchenOrder.getPreparedAt())
                .chefId(kitchenOrder.getChefId())
                .chefName(kitchenOrder.getChefName())
                .tickets(ticketDTOs)
                .build();
    }

    /**
     * Chuyển đổi Ticket thành DTO
     */
    private TicketDTO convertToDTO(Ticket ticket) {
        return TicketDTO.builder()
                .id(ticket.getId())
                .itemName(ticket.getItemName())
                .notes(ticket.getNotes())
                .status(ticket.getStatus())
                .createdAt(ticket.getCreatedAt())
                .completedAt(ticket.getCompletedAt())
                .build();
    }
}
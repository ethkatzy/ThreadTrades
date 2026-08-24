package com.threadtrades.admin;

import com.threadtrades.common.PagedResponse;
import com.threadtrades.message.MessageNotFoundException;
import com.threadtrades.message.MessageRepository;
import com.threadtrades.message.MessageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminMessageService {

    private final MessageRepository messageRepository;

    public AdminMessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<MessageResponse> list(int page, int size, Long matchId) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt"));
        var messages =
                matchId == null ? messageRepository.findAll(pageable) : messageRepository.findByMatchId(matchId, pageable);
        return PagedResponse.from(messages.map(MessageResponse::from));
    }

    @Transactional
    public void delete(Long id) {
        if (!messageRepository.existsById(id)) {
            throw new MessageNotFoundException(id);
        }
        messageRepository.deleteById(id);
    }
}

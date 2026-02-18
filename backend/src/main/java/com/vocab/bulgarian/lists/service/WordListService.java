package com.vocab.bulgarian.lists.service;

import com.vocab.bulgarian.api.dto.LemmaResponseDTO;
import com.vocab.bulgarian.api.mapper.LemmaMapper;
import com.vocab.bulgarian.domain.Lemma;
import com.vocab.bulgarian.lists.domain.WordList;
import com.vocab.bulgarian.lists.dto.*;
import com.vocab.bulgarian.lists.repository.WordListRepository;
import com.vocab.bulgarian.repository.LemmaRepository;
import com.vocab.bulgarian.study.domain.*;
import com.vocab.bulgarian.study.dto.*;
import com.vocab.bulgarian.study.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class WordListService {

    private static final Logger log = LoggerFactory.getLogger(WordListService.class);
    private static final int MAX_NEW_CARDS_PER_SESSION = 10;

    private final WordListRepository listRepo;
    private final LemmaRepository lemmaRepo;
    private final SrsStateRepository srsRepo;
    private final StudySessionRepository sessionRepo;
    private final SessionCardRepository cardRepo;
    private final LemmaMapper lemmaMapper;

    public WordListService(WordListRepository listRepo, LemmaRepository lemmaRepo,
                           SrsStateRepository srsRepo, StudySessionRepository sessionRepo,
                           SessionCardRepository cardRepo, LemmaMapper lemmaMapper) {
        this.listRepo = listRepo;
        this.lemmaRepo = lemmaRepo;
        this.srsRepo = srsRepo;
        this.sessionRepo = sessionRepo;
        this.cardRepo = cardRepo;
        this.lemmaMapper = lemmaMapper;
    }

    @Transactional(readOnly = true)
    public List<WordListSummaryDTO> getAllLists() {
        return listRepo.findAllOrderByName().stream()
            .map(wl -> new WordListSummaryDTO(
                wl.getId(), wl.getName(),
                listRepo.countLemmasByListId(wl.getId()),
                wl.getCreatedAt()))
            .toList();
    }

    @Transactional(readOnly = true)
    public WordListDetailDTO getListDetail(Long id) {
        WordList wl = listRepo.findByIdWithLemmas(id)
            .orElseThrow(() -> new EntityNotFoundException("List not found: " + id));
        List<LemmaResponseDTO> lemmas = wl.getLemmas().stream()
            .map(lemmaMapper::toResponseDTO)
            .sorted(Comparator.comparing(LemmaResponseDTO::text))
            .toList();
        return new WordListDetailDTO(wl.getId(), wl.getName(), lemmas, wl.getCreatedAt());
    }

    public WordListSummaryDTO createList(CreateWordListRequestDTO request) {
        WordList wl = new WordList();
        wl.setName(request.name().strip());
        wl = listRepo.save(wl);
        return new WordListSummaryDTO(wl.getId(), wl.getName(), 0, wl.getCreatedAt());
    }

    public WordListSummaryDTO renameList(Long id, RenameWordListRequestDTO request) {
        WordList wl = listRepo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("List not found: " + id));
        wl.setName(request.name().strip());
        listRepo.save(wl);
        return new WordListSummaryDTO(wl.getId(), wl.getName(),
            listRepo.countLemmasByListId(id), wl.getCreatedAt());
    }

    public void deleteList(Long id) {
        if (!listRepo.existsById(id)) {
            throw new EntityNotFoundException("List not found: " + id);
        }
        listRepo.deleteById(id);
    }

    public void addLemma(Long listId, Long lemmaId) {
        if (!listRepo.existsById(listId)) throw new EntityNotFoundException("List not found: " + listId);
        if (!lemmaRepo.existsById(lemmaId)) throw new EntityNotFoundException("Lemma not found: " + lemmaId);
        listRepo.addMember(listId, lemmaId);
    }

    public void removeLemma(Long listId, Long lemmaId) {
        if (!listRepo.existsById(listId)) throw new EntityNotFoundException("List not found: " + listId);
        listRepo.removeMember(listId, lemmaId);
    }

    public StartSessionResponseDTO startListSession(Long listId, String mode, int maxCards) {
        List<Long> listLemmaIds = listRepo.findLemmaIdsByListId(listId);
        if (listLemmaIds.isEmpty()) {
            throw new IllegalStateException("List is empty — add vocabulary before studying.");
        }

        Set<Long> listSet = new HashSet<>(listLemmaIds);
        List<Long> sessionIds;

        if ("ALL".equalsIgnoreCase(mode)) {
            sessionIds = new ArrayList<>(listLemmaIds);
            Collections.shuffle(sessionIds);
            sessionIds = sessionIds.stream().limit(maxCards).toList();
        } else {
            // DUE mode: only list members that are due or new
            List<Long> dueIds = srsRepo.findDueCards(LocalDate.now()).stream()
                .map(s -> s.getLemma().getId())
                .filter(listSet::contains)
                .toList();
            List<Long> newIds = srsRepo.findLemmaIdsWithoutSrsState().stream()
                .filter(listSet::contains)
                .limit(MAX_NEW_CARDS_PER_SESSION)
                .toList();
            Set<Long> merged = new LinkedHashSet<>(dueIds);
            merged.addAll(newIds);
            sessionIds = new ArrayList<>(merged);
            Collections.shuffle(sessionIds);
            sessionIds = sessionIds.stream().limit(maxCards).toList();
            if (sessionIds.isEmpty()) {
                throw new IllegalStateException(
                    "No due or new cards in this list. Try Practice All mode.");
            }
        }

        // Ensure SrsState exists for any new cards
        for (Long lemmaId : sessionIds) {
            if (srsRepo.findByLemmaId(lemmaId).isEmpty()) {
                SrsState state = new SrsState();
                state.setLemma(lemmaRepo.getReferenceById(lemmaId));
                srsRepo.save(state);
            }
        }

        log.info("List {} session started: mode={}, {} cards", listId, mode, sessionIds.size());
        return buildSession(sessionIds);
    }

    private StartSessionResponseDTO buildSession(List<Long> sessionIds) {
        StudySession session = new StudySession();
        session.setCardCount(sessionIds.size());
        session = sessionRepo.save(session);

        for (int i = 0; i < sessionIds.size(); i++) {
            Lemma lemma = lemmaRepo.getReferenceById(sessionIds.get(i));
            SessionCard card = new SessionCard();
            card.setSession(session);
            card.setLemma(lemma);
            card.setPosition(i);
            cardRepo.save(card);
        }

        final Long sessionId = session.getId();
        final int cardCount = session.getCardCount();
        StudyCardDTO firstCard = cardRepo.findFirstUnreviewed(sessionId, PageRequest.of(0, 1))
            .stream().findFirst()
            .map(sc -> new StudyCardDTO(
                sc.getLemma().getId(), sc.getLemma().getText(),
                sc.getLemma().getTranslation(), (long) cardCount))
            .orElse(null);

        return new StartSessionResponseDTO(sessionId, cardCount, firstCard);
    }
}

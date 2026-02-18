package com.vocab.bulgarian.lists.dto;

import com.vocab.bulgarian.api.dto.LemmaResponseDTO;

import java.time.ZonedDateTime;
import java.util.List;

public record WordListDetailDTO(Long id, String name, List<LemmaResponseDTO> lemmas, ZonedDateTime createdAt) {}

package com.sharenote.note;

public class ProposalNotFoundException extends RuntimeException {
    public ProposalNotFoundException(Long id) {
        super("Proposal not found with id: " + id);
    }
}

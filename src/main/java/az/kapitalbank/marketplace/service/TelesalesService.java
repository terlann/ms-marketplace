package az.kapitalbank.marketplace.service;

import java.util.Optional;

public interface TelesalesService {

    Optional<String> sendLead(String trackId);

}

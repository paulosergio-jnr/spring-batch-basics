package br.com.examples.springbatchbasics.processors;

import br.com.examples.springbatchbasics.domains.Customer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Service;

@Service
public class CustomerEnrichmentItemProcessor implements ItemProcessor<Customer, Customer> {

    private static final String UNITED_STATES_DESCRIPTION = "United States";

    @Override
    public Customer process(Customer customer) throws Exception {
        customer.setForeign(UNITED_STATES_DESCRIPTION.equals(customer.getAddress().getCountry()));

        return customer;
    }

}

package br.com.inventorymed.bootstrap;

import br.com.inventorymed.identity.AppUser;
import br.com.inventorymed.identity.AppUserRepository;
import br.com.inventorymed.identity.Hospital;
import br.com.inventorymed.identity.HospitalMembership;
import br.com.inventorymed.identity.HospitalMembershipRepository;
import br.com.inventorymed.identity.HospitalRepository;
import br.com.inventorymed.identity.HospitalRole;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DevelopmentDataInitializer implements ApplicationRunner {

    private final BootstrapProperties properties;
    private final HospitalRepository hospitalRepository;
    private final AppUserRepository userRepository;
    private final HospitalMembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;

    public DevelopmentDataInitializer(
        BootstrapProperties properties,
        HospitalRepository hospitalRepository,
        AppUserRepository userRepository,
        HospitalMembershipRepository membershipRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.properties = properties;
        this.hospitalRepository = hospitalRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) return;

        Hospital upa = findOrCreateHospital("UPA DE JOÃO PINHEIRO", "UPA JP");
        Hospital hospital = findOrCreateHospital("HOSPITAL DE JOÃO PINHEIRO", "HJP");
        AppUser doctor = userRepository
            .findByEmailIgnoreCase(properties.doctorEmail())
            .orElseGet(() ->
                userRepository.save(
                    new AppUser(
                        "LUCAS GALANTE",
                        properties.doctorEmail(),
                        passwordEncoder.encode(properties.doctorPassword())
                    )
                )
            );

        List.of(upa, hospital).forEach(item -> {
            if (!membershipRepository.existsByUserIdAndHospitalId(doctor.getId(), item.getId())) {
                membershipRepository.save(
                    new HospitalMembership(item, doctor, HospitalRole.DOCTOR)
                );
            }
        });
    }

    private Hospital findOrCreateHospital(String name, String shortName) {
        return hospitalRepository
            .findByNameIgnoreCase(name)
            .orElseGet(() ->
                hospitalRepository.save(new Hospital(name, shortName, "JOÃO PINHEIRO, MG"))
            );
    }
}

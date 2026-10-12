package com.medicareai.config;

import com.medicareai.entity.*;
import com.medicareai.enums.Role;
import com.medicareai.enums.StockMovementType;
import com.medicareai.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final CustomerProfileRepository profileRepository;
    private final CartRepository cartRepository;
    private final DeliveryAddressRepository addressRepository;
    private final MedicineCategoryRepository categoryRepository;
    private final MedicineRepository medicineRepository;
    private final SupplierRepository supplierRepository;
    private final MedicineBatchRepository batchRepository;
    private final StockMovementRepository movementRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initUsers();
        initCategoriesAndMedicines();
    }

    private void initUsers() {
        if (!userRepository.existsByEmail("admin@medicare.ai")) {
            User admin = User.builder()
                    .fullName("MediCare Administrator")
                    .email("admin@medicare.ai")
                    .password(passwordEncoder.encode("Admin@123"))
                    .phone("+91 9876543210")
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build();
            userRepository.save(admin);
            logger.info("Created default system administrator: admin@medicare.ai / Admin@123");
        }

        if (!userRepository.existsByEmail("pharmacist@medicare.ai")) {
            User pharma = User.builder()
                    .fullName("Chief Pharmacist Dr. Sarah Mehta")
                    .email("pharmacist@medicare.ai")
                    .password(passwordEncoder.encode("Pharma@123"))
                    .phone("+91 9876543211")
                    .role(Role.PHARMACY_ADMIN)
                    .enabled(true)
                    .build();
            userRepository.save(pharma);
            logger.info("Created default pharmacy administrator: pharmacist@medicare.ai / Pharma@123");
        }

        if (!userRepository.existsByEmail("customer@medicare.ai")) {
            User customer = User.builder()
                    .fullName("Rohan Sharma")
                    .email("customer@medicare.ai")
                    .password(passwordEncoder.encode("Customer@123"))
                    .phone("+91 9876543212")
                    .role(Role.CUSTOMER)
                    .enabled(true)
                    .build();
            User savedCustomer = userRepository.save(customer);

            CustomerProfile profile = CustomerProfile.builder()
                    .user(savedCustomer)
                    .dateOfBirth(LocalDate.of(1992, 5, 14))
                    .gender("Male")
                    .build();
            profileRepository.save(profile);

            Cart cart = Cart.builder().user(savedCustomer).build();
            cartRepository.save(cart);

            DeliveryAddress addr = DeliveryAddress.builder()
                    .user(savedCustomer)
                    .recipientName("Rohan Sharma")
                    .phone("+91 9876543212")
                    .addressLine1("Flat 402, Green Meadows, 5th Main Road")
                    .addressLine2("Koramangala 4th Block")
                    .city("Bengaluru")
                    .state("Karnataka")
                    .postalCode("560034")
                    .country("India")
                    .defaultAddress(true)
                    .build();
            addressRepository.save(addr);

            logger.info("Created default customer: customer@medicare.ai / Customer@123");
        }
    }

    private void initCategoriesAndMedicines() {
        if (medicineRepository.count() >= 95) {
            return;
        }

        logger.info("Initializing extensive pharmaceutical catalogue, suppliers, and FEFO inventory batches...");

        // Categories
        MedicineCategory painCat = getOrCreateCategory("Pain Relief & Fever",
                "Analgesics, antipyretics, and anti-inflammatory medications for pain and fever management.");

        MedicineCategory cardioCat = getOrCreateCategory("Cardiovascular & Heart Health",
                "Medications for hypertension, cholesterol, arrhythmia, and heart wellness.");

        MedicineCategory diabetesCat = getOrCreateCategory("Diabetes Care",
                "Blood sugar control tablets, insulin, test strips, and diabetic management.");

        MedicineCategory antibioticsCat = getOrCreateCategory("Antibiotics & Anti-Infectives",
                "Broad-spectrum antibacterial and antifungal prescription therapies.");

        MedicineCategory respCat = getOrCreateCategory("Respiratory & Allergy",
                "Antihistamines, inhalers, cough syrups, and decongestants.");

        MedicineCategory gastroCat = getOrCreateCategory("Gastrointestinal & Digestion",
                "Antacids, PPIs, anti-ulcer medications, probiotics, and digestive relief.");

        MedicineCategory vitCat = getOrCreateCategory("Vitamins & Supplements",
                "Multivitamins, immunity boosters, calcium, vitamin D, and health essentials.");

        MedicineCategory firstAidCat = getOrCreateCategory("First Aid & Devices",
                "Digital thermometers, antiseptic lotions, wound dressings, and pulse oximeters.");

        MedicineCategory skinCat = getOrCreateCategory("Dermatology & Skin Care",
                "Topical antiseptic ointments, healing creams, and skincare therapies.");

        MedicineCategory eyeOralCat = getOrCreateCategory("Eye, Ear & Oral Care",
                "Sterile lubricating ophthalmic drops, antiseptic ear drops, and specialized oral therapies.");

        // Suppliers
        if (supplierRepository.count() == 0) {
            supplierRepository.save(Supplier.builder()
                    .name("Apex Pharma Distributors Ltd.")
                    .contactPerson("Virendra Singh")
                    .email("orders@apexpharma.com")
                    .phone("+91 80 4123 9900")
                    .address("Plot 12, Peenya Industrial Area, Bengaluru, Karnataka")
                    .active(true).build());

            supplierRepository.save(Supplier.builder()
                    .name("Cipla Med Supply Network")
                    .contactPerson("Anita Nambiar")
                    .email("supply@ciplanetwork.in")
                    .phone("+91 22 2845 1100")
                    .address("Mumbai Central Business District, Mumbai, Maharashtra")
                    .active(true).build());

            supplierRepository.save(Supplier.builder()
                    .name("Sun Healthcare Logistics")
                    .contactPerson("Rajeev Saxena")
                    .email("logistics@sunhealth.co.in")
                    .phone("+91 11 4982 7700")
                    .address("Okhla Industrial Phase III, New Delhi")
                    .active(true).build());
        }

        // 1. Pain Relief & Fever
        createMedWithBatch(painCat, "Dolo 650 Tablet", "Micro Labs", "Paracetamol", "Micro Labs Ltd.",
                "Fast-acting relief for mild to moderate pain, body ache, and high fever reduction.",
                "Tablet", "650mg", "15 Tablets / Strip",
                "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=500&auto=format&fit=crop&q=60",
                false, "DOLO-2401", LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(21),
                BigDecimal.valueOf(22.00), BigDecimal.valueOf(31.50), 250);

        createMedWithBatch(painCat, "Crocin 500 Advance", "Crocin", "Paracetamol", "GlaxoSmithKline",
                "Optizorb technology paracetamol for quick relief from headaches and fever.",
                "Tablet", "500mg", "20 Tablets / Strip",
                "https://images.unsplash.com/photo-1587854692152-cbe660dbde88?w=500&auto=format&fit=crop&q=60",
                false, "CRC-8891", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(18),
                BigDecimal.valueOf(18.50), BigDecimal.valueOf(26.00), 200);

        createMedWithBatch(painCat, "Combiflam Tablet", "Sanofi", "Ibuprofen + Paracetamol", "Sanofi India",
                "Combination analgesic and anti-inflammatory tablet for joint and muscular pain.",
                "Tablet", "400mg + 325mg", "20 Tablets / Strip",
                "https://images.unsplash.com/photo-1471864190281-a93a3070b6de?w=500&auto=format&fit=crop&q=60",
                false, "CMB-7720", LocalDate.now().minusMonths(4), LocalDate.now().plusMonths(16),
                BigDecimal.valueOf(32.00), BigDecimal.valueOf(45.00), 180);

        createMedWithBatch(painCat, "Saridon Headache Relief", "Piramal", "Paracetamol + Propyphenazone + Caffeine", "Piramal Pharma",
                "Triple action formula designed specifically for targeted and rapid headache relief.",
                "Tablet", "250mg + 150mg + 50mg", "10 Tablets / Strip",
                "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=500&auto=format&fit=crop&q=60",
                false, "SRD-1021", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(22),
                BigDecimal.valueOf(28.00), BigDecimal.valueOf(42.00), 220);

        createMedWithBatch(painCat, "Volini Pain Relief Gel", "Sun Pharma", "Diclofenac Diethylamine + Methyl Salicylate", "Sun Pharma",
                "Quick absorbing topical gel providing instant relief from sprains, backaches and joint stiffness.",
                "Gel", "50g", "50g Squeeze Tube",
                "https://images.unsplash.com/photo-1556228720-195a672e8a03?w=500&auto=format&fit=crop&q=60",
                false, "VOL-5532", LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(24),
                BigDecimal.valueOf(105.00), BigDecimal.valueOf(145.00), 150);

        createMedWithBatch(painCat, "Meftal-Spas Tablet", "Blue Cross", "Mefenamic Acid + Dicyclomine", "Blue Cross Labs",
                "Antispasmodic and analgesic medication effective against abdominal cramps and spasms.",
                "Tablet", "250mg + 10mg", "10 Tablets / Strip",
                "https://images.unsplash.com/photo-1584017911766-d451b3d0e843?w=500&auto=format&fit=crop&q=60",
                false, "MFT-4410", LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(20),
                BigDecimal.valueOf(38.00), BigDecimal.valueOf(52.00), 190);

        createMedWithBatch(painCat, "Disprin Regular 325mg", "Reckitt", "Soluble Aspirin", "Reckitt Benckiser",
                "Fast-dissolving effervescent analgesic for relief from acute headaches and fever.",
                "Tablet", "325mg", "10 Tablets / Strip",
                "https://images.unsplash.com/photo-1587854692152-cbe660dbde88?w=500&auto=format&fit=crop&q=60",
                false, "DSP-3301", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(18),
                BigDecimal.valueOf(10.00), BigDecimal.valueOf(15.00), 300);

        createMedWithBatch(painCat, "Voveran 50 GE Tablet", "Novartis", "Diclofenac Sodium", "Novartis Healthcare",
                "Gastro-resistant anti-inflammatory tablet for persistent joint, bone, and dental pain.",
                "Tablet", "50mg", "15 Tablets / Strip",
                "https://images.unsplash.com/photo-1471864190281-a93a3070b6de?w=500&auto=format&fit=crop&q=60",
                true, "VOV-9918", LocalDate.now().minusMonths(4), LocalDate.now().plusMonths(20),
                BigDecimal.valueOf(58.00), BigDecimal.valueOf(85.00), 160);

        // 2. Antibiotics & Anti-Infectives
        createMedWithBatch(antibioticsCat, "Augmentin 625 Duo", "Augmentin", "Amoxicillin + Clavulanic Acid", "GlaxoSmithKline",
                "Broad spectrum antibiotic used to treat bacterial infections of ears, throat, lungs and skin.",
                "Tablet", "500mg + 125mg", "10 Tablets / Strip",
                "https://images.unsplash.com/photo-1628771065518-0d82f1938462?w=500&auto=format&fit=crop&q=60",
                true, "AUG-9912", LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(14),
                BigDecimal.valueOf(140.00), BigDecimal.valueOf(204.00), 120);

        createMedWithBatch(antibioticsCat, "Azithral 500 Tablet", "Alembic", "Azithromycin", "Alembic Pharmaceuticals",
                "Macrolide antibiotic indicated for bacterial respiratory infections and sinusitis.",
                "Tablet", "500mg", "5 Tablets / Strip",
                "https://images.unsplash.com/photo-1550572017-edd951aa8f72?w=500&auto=format&fit=crop&q=60",
                true, "AZI-4432", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(20),
                BigDecimal.valueOf(88.00), BigDecimal.valueOf(128.00), 150);

        createMedWithBatch(antibioticsCat, "Ciplox 500 Tablet", "Cipla", "Ciprofloxacin", "Cipla Limited",
                "Fluoroquinolone antibiotic for urinary tract, skin, and abdominal bacterial infections.",
                "Tablet", "500mg", "10 Tablets / Strip",
                "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=500&auto=format&fit=crop&q=60",
                true, "CIP-7711", LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(21),
                BigDecimal.valueOf(30.00), BigDecimal.valueOf(44.00), 180);

        createMedWithBatch(antibioticsCat, "Taxim-O 200 Tablet", "Alkem", "Cefixime", "Alkem Laboratories",
                "Third-generation cephalosporin for throat, ear, and respiratory tract infections.",
                "Tablet", "200mg", "10 Tablets / Strip",
                "https://images.unsplash.com/photo-1628771065518-0d82f1938462?w=500&auto=format&fit=crop&q=60",
                true, "TXM-8822", LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(19),
                BigDecimal.valueOf(78.00), BigDecimal.valueOf(115.00), 140);

        createMedWithBatch(antibioticsCat, "Moxikind-CV 625", "Mankind", "Amoxicillin + Clavulanic Acid", "Mankind Pharma",
                "High efficacy antibiotic for dental abscesses, pneumonia, and bacterial skin infections.",
                "Tablet", "500mg + 125mg", "10 Tablets / Strip",
                "https://images.unsplash.com/photo-1550572017-edd951aa8f72?w=500&auto=format&fit=crop&q=60",
                true, "MOX-4421", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(18),
                BigDecimal.valueOf(125.00), BigDecimal.valueOf(180.00), 130);

        createMedWithBatch(antibioticsCat, "Norflox TZ Tablet", "Cipla", "Norfloxacin + Tinidazole", "Cipla Limited",
                "Dual-action antimicrobial and antiprotozoal tablet for gastrointestinal and stomach infections.",
                "Tablet", "400mg + 600mg", "10 Tablets / Strip",
                "https://images.unsplash.com/photo-1584017911766-d451b3d0e843?w=500&auto=format&fit=crop&q=60",
                true, "NFZ-3312", LocalDate.now().minusMonths(4), LocalDate.now().plusMonths(16),
                BigDecimal.valueOf(65.00), BigDecimal.valueOf(95.00), 170);

        // 3. Cardiovascular & Heart Health
        createMedWithBatch(cardioCat, "Telma 40 Tablet", "Glenmark", "Telmisartan", "Glenmark Pharmaceuticals",
                "Angiotensin receptor blocker used for hypertension and cardiovascular risk prevention.",
                "Tablet", "40mg", "30 Tablets / Box",
                "https://images.unsplash.com/photo-1579165466791-788226ab77b6?w=500&auto=format&fit=crop&q=60",
                true, "TEL-6601", LocalDate.now().minusMonths(5), LocalDate.now().plusMonths(19),
                BigDecimal.valueOf(155.00), BigDecimal.valueOf(220.00), 160);

        createMedWithBatch(cardioCat, "Rosuvas 10 Tablet", "Sun Pharma", "Rosuvastatin", "Sun Pharmaceutical Industries",
                "Statin medication to lower LDL bad cholesterol and reduce heart disease risk.",
                "Tablet", "10mg", "15 Tablets / Strip",
                "https://images.unsplash.com/photo-1584017911766-d451b3d0e843?w=500&auto=format&fit=crop&q=60",
                true, "RSV-1205", LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(17),
                BigDecimal.valueOf(180.00), BigDecimal.valueOf(265.00), 140);

        createMedWithBatch(cardioCat, "Amlong 5 Tablet", "Micro Labs", "Amlodipine", "Micro Labs Ltd.",
                "Calcium channel blocker prescribed for hypertension management and angina prevention.",
                "Tablet", "5mg", "15 Tablets / Strip",
                "https://images.unsplash.com/photo-1587854692152-cbe660dbde88?w=500&auto=format&fit=crop&q=60",
                true, "AML-5511", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(22),
                BigDecimal.valueOf(28.00), BigDecimal.valueOf(42.00), 240);

        createMedWithBatch(cardioCat, "Atorva 20 Tablet", "Zydus", "Atorvastatin", "Zydus Healthcare",
                "Lipid-lowering medication supporting arterial health and lowering cardiac events.",
                "Tablet", "20mg", "15 Tablets / Strip",
                "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=500&auto=format&fit=crop&q=60",
                true, "ATV-8820", LocalDate.now().minusMonths(4), LocalDate.now().plusMonths(18),
                BigDecimal.valueOf(145.00), BigDecimal.valueOf(215.00), 150);

        createMedWithBatch(cardioCat, "Concor 5 Tablet", "Merck", "Bisoprolol Fumarate", "Merck Specialities",
                "Beta-blocker reducing blood pressure and heart workload for steady cardiovascular rhythm.",
                "Tablet", "5mg", "10 Tablets / Strip",
                "https://images.unsplash.com/photo-1471864190281-a93a3070b6de?w=500&auto=format&fit=crop&q=60",
                true, "CNC-2204", LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(23),
                BigDecimal.valueOf(95.00), BigDecimal.valueOf(135.00), 110);

        createMedWithBatch(cardioCat, "Ecosprin 75 Tablet", "USV", "Aspirin Enteric Coated", "USV Private Limited",
                "Antiplatelet agent preventing blood clots in patients with coronary heart disease.",
                "Tablet", "75mg", "14 Tablets / Strip",
                "https://images.unsplash.com/photo-1584017911766-d451b3d0e843?w=500&auto=format&fit=crop&q=60",
                false, "ECP-9910", LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(21),
                BigDecimal.valueOf(8.50), BigDecimal.valueOf(12.50), 350);

        // 4. Diabetes Care
        createMedWithBatch(diabetesCat, "Glycomet 500 SR", "USV", "Metformin Hydrochloride", "USV Private Limited",
                "Sustained-release biguanide for type 2 diabetes mellitus glycemic management.",
                "Tablet", "500mg", "20 Tablets / Strip",
                "https://images.unsplash.com/photo-1607613009820-a29f7bb81c04?w=500&auto=format&fit=crop&q=60",
                true, "GLY-3310", LocalDate.now().minusMonths(4), LocalDate.now().plusMonths(22),
                BigDecimal.valueOf(38.00), BigDecimal.valueOf(54.00), 220);

        createMedWithBatch(diabetesCat, "Januvia 100mg Tablet", "MSD", "Sitagliptin", "Merck Sharp & Dohme",
                "DPP-4 inhibitor enhancing natural incretins to maintain balanced blood glucose levels.",
                "Tablet", "100mg", "7 Tablets / Strip",
                "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=500&auto=format&fit=crop&q=60",
                true, "JNV-4401", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(20),
                BigDecimal.valueOf(280.00), BigDecimal.valueOf(385.00), 90);

        createMedWithBatch(diabetesCat, "Amaryl 2mg Tablet", "Sanofi", "Glimepiride", "Sanofi India",
                "Sulfonylurea anti-diabetic tablet that stimulates natural insulin release from the pancreas.",
                "Tablet", "2mg", "15 Tablets / Strip",
                "https://images.unsplash.com/photo-1587854692152-cbe660dbde88?w=500&auto=format&fit=crop&q=60",
                true, "AMR-6612", LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(19),
                BigDecimal.valueOf(135.00), BigDecimal.valueOf(195.00), 140);

        createMedWithBatch(diabetesCat, "Galvus Met 50/500", "Novartis", "Vildagliptin + Metformin", "Novartis Healthcare",
                "Dual action glucose regulator combining a DPP-4 inhibitor with metformin hydrochloride.",
                "Tablet", "50mg + 500mg", "10 Tablets / Strip",
                "https://images.unsplash.com/photo-1607613009820-a29f7bb81c04?w=500&auto=format&fit=crop&q=60",
                true, "GLV-9932", LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(21),
                BigDecimal.valueOf(210.00), BigDecimal.valueOf(290.00), 120);

        createMedWithBatch(diabetesCat, "Accu-Chek Active 50 Strips", "Roche", "Blood Glucose Test Strips", "Roche Diabetes Care",
                "Accurate self-monitoring blood glucose test strips for regular diabetic sugar logging.",
                "Device", "50 Strips", "50 Strips Box",
                "https://images.unsplash.com/photo-1584515979956-d9f6e5d09982?w=500&auto=format&fit=crop&q=60",
                false, "ACK-5520", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(22),
                BigDecimal.valueOf(740.00), BigDecimal.valueOf(975.00), 80);

        // 5. Respiratory & Allergy
        createMedWithBatch(respCat, "Allegra 120mg", "Sanofi", "Fexofenadine Hydrochloride", "Sanofi Healthcare",
                "Non-drowsy antihistamine for allergic rhinitis, sneezing, watery eyes, and hives.",
                "Tablet", "120mg", "10 Tablets / Strip",
                "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=500&auto=format&fit=crop&q=60",
                false, "ALG-5509", LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(23),
                BigDecimal.valueOf(142.00), BigDecimal.valueOf(198.00), 190);

        createMedWithBatch(respCat, "Montair-LC Tablet", "Cipla", "Montelukast + Levocetirizine", "Cipla Limited",
                "Dual action anti-allergy and leukotriene receptor antagonist for seasonal asthma and rhinitis.",
                "Tablet", "10mg + 5mg", "10 Tablets / Strip",
                "https://images.unsplash.com/photo-1550572017-edd951aa8f72?w=500&auto=format&fit=crop&q=60",
                false, "MNT-7721", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(22),
                BigDecimal.valueOf(120.00), BigDecimal.valueOf(175.00), 210);

        createMedWithBatch(respCat, "Ascoril D Plus Cough Syrup", "Glenmark", "Dextromethorphan + Chlorpheniramine + Phenylephrine", "Glenmark Pharmaceuticals",
                "Sugar-free cough syrup providing relief from dry allergic cough and blocked nasal passages.",
                "Syrup", "100ml", "100ml Bottle",
                "https://images.unsplash.com/photo-1603555501671-8f96b3fce8e4?w=500&auto=format&fit=crop&q=60",
                false, "ASC-8833", LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(18),
                BigDecimal.valueOf(82.00), BigDecimal.valueOf(118.00), 140);

        createMedWithBatch(respCat, "Benadryl Cough Formula", "Benadryl", "Diphenhydramine + Ammonium Chloride", "Johnson & Johnson",
                "Soothing cough syrup with active throat demulcent and expectorant properties.",
                "Syrup", "150ml", "150ml Bottle",
                "https://images.unsplash.com/photo-1603555501671-8f96b3fce8e4?w=500&auto=format&fit=crop&q=60",
                false, "BND-4411", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(20),
                BigDecimal.valueOf(92.00), BigDecimal.valueOf(132.00), 160);

        createMedWithBatch(respCat, "Asthalin Inhaler 100mcg", "Cipla", "Salbutamol Inhaler", "Cipla Limited",
                "Bronchodilator aerosol inhaler offering fast relief from acute asthma and bronchospasms.",
                "Inhaler", "100mcg", "200 Actuations Canister",
                "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=500&auto=format&fit=crop&q=60",
                true, "AST-6620", LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(21),
                BigDecimal.valueOf(115.00), BigDecimal.valueOf(158.00), 130);

        createMedWithBatch(respCat, "Otrivin Oxy Fast Relief Spray", "Otrivin", "Oxymetazoline Hydrochloride", "GlaxoSmithKline",
                "Fast acting nasal decongestant spray relieving blocked sinuses and seasonal congestion in 25 seconds.",
                "Spray", "0.05% w/v", "10ml Nasal Spray Bottle",
                "https://images.unsplash.com/photo-1603555501671-8f96b3fce8e4?w=500&auto=format&fit=crop&q=60",
                false, "OTR-1120", LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(23),
                BigDecimal.valueOf(75.00), BigDecimal.valueOf(105.00), 200);

        createMedWithBatch(respCat, "Vicks VapoRub 50ml", "Procter & Gamble", "Menthol + Camphor + Eucalyptus", "Procter & Gamble",
                "Ayurvedic topical vaporizing ointment for multi-symptom cold, cough, and chest stiffness relief.",
                "Ointment", "50ml", "50ml Jar",
                "https://images.unsplash.com/photo-1556228720-195a672e8a03?w=500&auto=format&fit=crop&q=60",
                false, "VCK-9904", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(30),
                BigDecimal.valueOf(105.00), BigDecimal.valueOf(145.00), 250);

        createMedWithBatch(respCat, "Cetriz 10mg Tablet", "Alkem", "Cetirizine Hydrochloride", "Alkem Laboratories",
                "Trusted antihistamine providing comprehensive relief from allergies, itching, and hives.",
                "Tablet", "10mg", "10 Tablets / Strip",
                "https://images.unsplash.com/photo-1587854692152-cbe660dbde88?w=500&auto=format&fit=crop&q=60",
                false, "CTZ-3310", LocalDate.now().minusMonths(4), LocalDate.now().plusMonths(20),
                BigDecimal.valueOf(14.00), BigDecimal.valueOf(22.00), 300);

        // 6. Gastrointestinal & Digestion
        createMedWithBatch(gastroCat, "Pan 40 Tablet", "Alkem", "Pantoprazole", "Alkem Laboratories",
                "Proton pump inhibitor (PPI) reducing stomach acid for GERD, reflux, and acidity.",
                "Tablet", "40mg", "15 Tablets / Strip",
                "https://images.unsplash.com/photo-1550572017-edd951aa8f72?w=500&auto=format&fit=crop&q=60",
                false, "PAN-8812", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(20),
                BigDecimal.valueOf(105.00), BigDecimal.valueOf(155.00), 210);

        createMedWithBatch(gastroCat, "Omez 20 Capsule", "Dr. Reddy's", "Omeprazole", "Dr. Reddy's Laboratories",
                "Proton pump inhibitor for heartburn relief, acid reflux control, and gastric ulcer healing.",
                "Capsule", "20mg", "20 Capsules / Strip",
                "https://images.unsplash.com/photo-1584017911766-d451b3d0e843?w=500&auto=format&fit=crop&q=60",
                false, "OMZ-2219", LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(21),
                BigDecimal.valueOf(42.00), BigDecimal.valueOf(62.00), 230);

        createMedWithBatch(gastroCat, "Digene Antacid Gel Orange", "Abbott", "Magnesium Hydroxide + Aluminium Hydroxide + Simethicone", "Abbott India",
                "Scientifically formulated antacid syrup giving speedy soothing relief from acid indigestion and gas.",
                "Liquid", "200ml", "200ml Bottle",
                "https://images.unsplash.com/photo-1603555501671-8f96b3fce8e4?w=500&auto=format&fit=crop&q=60",
                false, "DGN-7721", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(22),
                BigDecimal.valueOf(98.00), BigDecimal.valueOf(138.00), 180);

        createMedWithBatch(gastroCat, "Gelusil MPS Mint Liquid", "Pfizer", "Aluminium Hydroxide + Dimethicone", "Pfizer Limited",
                "Refreshing mint flavoured antacid and anti-flatulent liquid for quick heartburn alleviation.",
                "Liquid", "200ml", "200ml Bottle",
                "https://images.unsplash.com/photo-1603555501671-8f96b3fce8e4?w=500&auto=format&fit=crop&q=60",
                false, "GLS-4412", LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(23),
                BigDecimal.valueOf(88.00), BigDecimal.valueOf(124.00), 190);

        createMedWithBatch(gastroCat, "Pudin Hara Pearls", "Dabur", "Mentha Piperita + Spearmint Oil", "Dabur India",
                "Ayurvedic herbal cooling softgels providing immediate relief from stomachache, gas, and indigestion.",
                "Capsule", "Herbal Pearls", "10 Pearls / Strip",
                "https://images.unsplash.com/photo-1584017911766-d451b3d0e843?w=500&auto=format&fit=crop&q=60",
                false, "PDN-1102", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(24),
                BigDecimal.valueOf(24.00), BigDecimal.valueOf(35.00), 320);

        createMedWithBatch(gastroCat, "Cremaffin Plus Emulsion", "Abbott", "Liquid Paraffin + Milk of Magnesia", "Abbott India",
                "Gentle and effective laxative emulsion providing comfortable overnight constipation relief.",
                "Liquid", "225ml", "225ml Bottle",
                "https://images.unsplash.com/photo-1603555501671-8f96b3fce8e4?w=500&auto=format&fit=crop&q=60",
                false, "CRF-8830", LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(18),
                BigDecimal.valueOf(140.00), BigDecimal.valueOf(198.00), 120);

        createMedWithBatch(gastroCat, "Sporlac DS Tablet", "Sanzyme", "Lactic Acid Bacillus (Probiotics)", "Sanzyme Biologics",
                "Probiotic spore therapy restoring healthy gut microflora balance following antibiotic treatments.",
                "Tablet", "120M Spores", "20 Tablets / Strip",
                "https://images.unsplash.com/photo-1587854692152-cbe660dbde88?w=500&auto=format&fit=crop&q=60",
                false, "SPL-5510", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(20),
                BigDecimal.valueOf(64.00), BigDecimal.valueOf(92.00), 160);

        // 7. Vitamins & Supplements
        createMedWithBatch(vitCat, "Becosules Z Capsules", "Pfizer", "Vitamin B-Complex + Vitamin C + Zinc", "Pfizer Limited",
                "Daily essential multivitamin and mineral supplement supporting immunity, metabolism, and energy.",
                "Capsule", "Zinc Formulated", "20 Capsules / Strip",
                "https://images.unsplash.com/photo-1584017911766-d451b3d0e843?w=500&auto=format&fit=crop&q=60",
                false, "BCS-7703", LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(15),
                BigDecimal.valueOf(35.00), BigDecimal.valueOf(50.00), 300);

        createMedWithBatch(vitCat, "Neurobion Forte Tablet", "P&G Health", "Vitamin B-Complex with B12", "Procter & Gamble Health",
                "Therapeutic B-vitamin complex supporting nerve health, regeneration, and tingling sensation relief.",
                "Tablet", "High Potency B12", "30 Tablets / Strip",
                "https://images.unsplash.com/photo-1587854692152-cbe660dbde88?w=500&auto=format&fit=crop&q=60",
                false, "NRB-2201", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(22),
                BigDecimal.valueOf(25.00), BigDecimal.valueOf(38.00), 280);

        createMedWithBatch(vitCat, "Shelcal 500 Tablet", "Torrent", "Calcium Carbonate + Vitamin D3", "Torrent Pharmaceuticals",
                "Pure elemental calcium with active vitamin D3 to enhance bone density and osteo-health.",
                "Tablet", "500mg + 250 IU", "15 Tablets / Strip",
                "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=500&auto=format&fit=crop&q=60",
                false, "SHL-9912", LocalDate.now().minusMonths(4), LocalDate.now().plusMonths(20),
                BigDecimal.valueOf(82.00), BigDecimal.valueOf(118.00), 210);

        createMedWithBatch(vitCat, "D-Rise 60K Capsule", "USV", "Cholecalciferol Vitamin D3", "USV Private Limited",
                "High strength weekly vitamin D3 soft gelatin capsules for deficiency recovery and immunity.",
                "Capsule", "60,000 IU", "4 Softgels / Strip",
                "https://images.unsplash.com/photo-1584017911766-d451b3d0e843?w=500&auto=format&fit=crop&q=60",
                false, "DRS-3319", LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(23),
                BigDecimal.valueOf(195.00), BigDecimal.valueOf(285.00), 170);

        createMedWithBatch(vitCat, "Limcee 500mg Chewable", "Abbott", "Vitamin C (Ascorbic Acid)", "Abbott India",
                "Tasty orange chewable vitamin C tablets promoting collagen synthesis and immune defense.",
                "Tablet", "500mg", "15 Tablets / Strip",
                "https://images.unsplash.com/photo-1587854692152-cbe660dbde88?w=500&auto=format&fit=crop&q=60",
                false, "LMC-4402", LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(21),
                BigDecimal.valueOf(16.00), BigDecimal.valueOf(24.00), 350);

        createMedWithBatch(vitCat, "Zincovit Tablet", "Apex Labs", "Multivitamins + Multiminerals + Grape Seed Extract", "Apex Laboratories",
                "Nutraceutical supplement rich in antioxidants, zinc, and micronutrients for daily stamina.",
                "Tablet", "Nutritional Blend", "15 Tablets / Strip",
                "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=500&auto=format&fit=crop&q=60",
                false, "ZNC-8811", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(22),
                BigDecimal.valueOf(74.00), BigDecimal.valueOf(105.00), 230);

        createMedWithBatch(vitCat, "Revital H Daily Health", "Sun Pharma", "Ginseng + 10 Vitamins + 9 Minerals", "Sun Pharmaceutical Industries",
                "Energy-boosting daily capsules improving physical endurance, mental alertness, and vitality.",
                "Capsule", "Ginseng Complex", "30 Capsules / Box",
                "https://images.unsplash.com/photo-1584017911766-d451b3d0e843?w=500&auto=format&fit=crop&q=60",
                false, "RVT-6610", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(22),
                BigDecimal.valueOf(220.00), BigDecimal.valueOf(310.00), 140);

        // 8. First Aid & Devices
        createMedWithBatch(firstAidCat, "Dr. Morepen Digital Thermometer", "Dr. Morepen", "Digital Medical Sensor", "Morepen Laboratories",
                "High-precision digital clinical thermometer with LCD display and fever beeper alarm.",
                "Device", "1 Unit", "Single Unit Box",
                "https://images.unsplash.com/photo-1584515979956-d9f6e5d09982?w=500&auto=format&fit=crop&q=60",
                false, "THM-2024", LocalDate.now().minusMonths(6), LocalDate.now().plusMonths(36),
                BigDecimal.valueOf(160.00), BigDecimal.valueOf(249.00), 85);

        createMedWithBatch(firstAidCat, "Dettol Antiseptic Liquid", "Reckitt", "Chloroxylenol", "Reckitt Benckiser",
                "Trusted first aid antiseptic liquid for wound cleansing, minor cuts, and personal hygiene.",
                "Liquid", "250ml", "250ml Bottle",
                "https://images.unsplash.com/photo-1603555501671-8f96b3fce8e4?w=500&auto=format&fit=crop&q=60",
                false, "DTL-9901", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(34),
                BigDecimal.valueOf(110.00), BigDecimal.valueOf(152.00), 120);

        createMedWithBatch(firstAidCat, "Betadine 10% Ointment", "Win-Medicare", "Povidone Iodine 10%", "Win-Medicare Pvt Ltd",
                "Broad-spectrum microbicidal water-soluble ointment for prevention of infections in burns and wounds.",
                "Ointment", "10% w/w", "20g Tube",
                "https://images.unsplash.com/photo-1556228720-195a672e8a03?w=500&auto=format&fit=crop&q=60",
                false, "BTD-7719", LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(21),
                BigDecimal.valueOf(78.00), BigDecimal.valueOf(112.00), 190);

        createMedWithBatch(skinCat, "Soframycin Skin Cream", "Sanofi", "Framycetin Sulphate 1%", "Sanofi India",
                "Topical antibacterial cream for superficial infections of burns, ulcers, and wounds.",
                "Cream", "1% w/w", "30g Tube",
                "https://images.unsplash.com/photo-1556228720-195a672e8a03?w=500&auto=format&fit=crop&q=60",
                false, "SFR-3310", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(22),
                BigDecimal.valueOf(40.00), BigDecimal.valueOf(58.00), 220);

        createMedWithBatch(skinCat, "Burnol Antiseptic Cream", "Morepen", "Aminacrine HCl + Cetrimide", "Morepen Laboratories",
                "Effective burn relief cream that relieves burn pain, combats germs and prevents blistering.",
                "Cream", "Antiseptic Formulation", "20g Tube",
                "https://images.unsplash.com/photo-1556228720-195a672e8a03?w=500&auto=format&fit=crop&q=60",
                false, "BNL-5512", LocalDate.now().minusMonths(4), LocalDate.now().plusMonths(20),
                BigDecimal.valueOf(46.00), BigDecimal.valueOf(68.00), 170);

        createMedWithBatch(firstAidCat, "Hansaplast Washproof Bandages", "Beiersdorf", "Medicated Wound Bandages", "Beiersdorf India",
                "Breathable washproof antiseptic adhesive bandages for everyday scratches, scrapes and cuts.",
                "Device", "20 Strips", "20 Strips Pack",
                "https://images.unsplash.com/photo-1584515979956-d9f6e5d09982?w=500&auto=format&fit=crop&q=60",
                false, "HNS-2211", LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(33),
                BigDecimal.valueOf(32.00), BigDecimal.valueOf(45.00), 260);

        createMedWithBatch(firstAidCat, "Ciplox Eye/Ear Drops", "Cipla", "Ciprofloxacin 0.3%", "Cipla Limited",
                "Sterile ophthalmic/otic solution for bacterial conjunctivitis and ear canal infections.",
                "Liquid", "0.3% w/v", "10ml Sterile Vial",
                "https://images.unsplash.com/photo-1603555501671-8f96b3fce8e4?w=500&auto=format&fit=crop&q=60",
                true, "CPD-9902", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(16),
                BigDecimal.valueOf(14.00), BigDecimal.valueOf(21.00), 210);

        // --- ADDITIONAL EXPANDED CATALOGUE MEDICINES ---

        // Pain Relief & Fever (Additional)
        createMedWithBatch(painCat, "Zerodol-SP Tablet", "Ipca Labs", "Aceclofenac + Paracetamol + Serratiopeptidase", "Ipca Laboratories Ltd.",
                "Triple-action anti-inflammatory pain relief for joint arthritis, muscle injury, and dental swelling.",
                "Tablet", "100mg + 325mg + 15mg", "10 Tablets / Strip",
                "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=500&auto=format&fit=crop&q=60",
                true, "ZDL-2211", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(22),
                BigDecimal.valueOf(75.00), BigDecimal.valueOf(115.00), 180);

        createMedWithBatch(painCat, "Calpol 500 Tablet", "GlaxoSmithKline", "Paracetamol", "GlaxoSmithKline Pharmaceuticals",
                "Trusted paediatric and adult formulation for fever control, post-immunization temperature, and pain.",
                "Tablet", "500mg", "15 Tablets / Strip",
                "https://images.unsplash.com/photo-1587854692152-cbe660dbde88?w=500&auto=format&fit=crop&q=60",
                false, "CLP-4409", LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(24),
                BigDecimal.valueOf(12.00), BigDecimal.valueOf(19.50), 250);

        createMedWithBatch(painCat, "Brufen 400 Tablet", "Abbott", "Ibuprofen", "Abbott India",
                "Non-steroidal anti-inflammatory drug (NSAID) relieving backache, toothache, and musculoskeletal strain.",
                "Tablet", "400mg", "15 Tablets / Strip",
                "https://images.unsplash.com/photo-1471864190281-a93a3070b6de?w=500&auto=format&fit=crop&q=60",
                false, "BRF-8812", LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(21),
                BigDecimal.valueOf(14.50), BigDecimal.valueOf(22.00), 200);

        createMedWithBatch(painCat, "Moov Pain Relief Spray", "Reckitt", "Ayurvedic Pain Formulation", "Reckitt Benckiser",
                "Fast-acting targeted aerosol spray formulated with wintergreen oil and mint for lower back pain.",
                "Spray", "50g Aerosol", "50g Spray Canister",
                "https://images.unsplash.com/photo-1556228720-195a672e8a03?w=500&auto=format&fit=crop&q=60",
                false, "MOV-3320", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(30),
                BigDecimal.valueOf(120.00), BigDecimal.valueOf(175.00), 140);

        createMedWithBatch(painCat, "Spasmo-Proxyvon Plus", "Wockhardt", "Tramadol + Dicyclomine + Acetaminophen", "Wockhardt Limited",
                "Prescription analgesic for acute postoperative pain, spasmodic cramps, and intense visceral distress.",
                "Capsule", "50mg + 10mg + 325mg", "8 Capsules / Strip",
                "https://images.unsplash.com/photo-1584017911766-d451b3d0e843?w=500&auto=format&fit=crop&q=60",
                true, "SPP-9901", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(18),
                BigDecimal.valueOf(52.00), BigDecimal.valueOf(78.00), 110);

        // Antibiotics & Anti-Infectives (Additional)
        createMedWithBatch(antibioticsCat, "Doxy-1 L-DR Forte", "USV", "Doxycycline + Lactic Acid Bacillus", "USV Private Limited",
                "Broad-spectrum tetracycline antibiotic for acne vulgaris, chest infections, and tick-borne diseases.",
                "Capsule", "100mg", "10 Capsules / Strip",
                "https://images.unsplash.com/photo-1628771065518-0d82f1938462?w=500&auto=format&fit=crop&q=60",
                true, "DOX-7714", LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(18),
                BigDecimal.valueOf(70.00), BigDecimal.valueOf(105.00), 130);

        createMedWithBatch(antibioticsCat, "Oflox 200 Tablet", "Cipla", "Ofloxacin", "Cipla Limited",
                "Bactericidal fluoroquinolone medication for enteric fever, gonococcal infections, and pelvic infections.",
                "Tablet", "200mg", "10 Tablets / Strip",
                "https://images.unsplash.com/photo-1550572017-edd951aa8f72?w=500&auto=format&fit=crop&q=60",
                true, "OFL-1102", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(22),
                BigDecimal.valueOf(48.00), BigDecimal.valueOf(72.00), 160);

        createMedWithBatch(antibioticsCat, "Flagyl 400 Tablet", "Abbott", "Metronidazole", "Abbott Healthcare",
                "Effective antimicrobial against anaerobic bacteria, dental abscesses, and amoebic dysentery.",
                "Tablet", "400mg", "15 Tablets / Strip",
                "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=500&auto=format&fit=crop&q=60",
                true, "FLG-5519", LocalDate.now().minusMonths(4), LocalDate.now().plusMonths(20),
                BigDecimal.valueOf(16.00), BigDecimal.valueOf(25.50), 220);

        createMedWithBatch(antibioticsCat, "Zifi 200 Tablet", "FDC", "Cefixime", "FDC Limited",
                "Oral cephalosporin antibiotic recommended for acute bronchitis, typhoid fever, and tonsillitis.",
                "Tablet", "200mg", "10 Tablets / Strip",
                "https://images.unsplash.com/photo-1628771065518-0d82f1938462?w=500&auto=format&fit=crop&q=60",
                true, "ZIF-8833", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(19),
                BigDecimal.valueOf(82.00), BigDecimal.valueOf(120.00), 140);

        createMedWithBatch(antibioticsCat, "Levomac 500 Tablet", "Macleods", "Levofloxacin", "Macleods Pharmaceuticals",
                "Third-generation fluoroquinolone antibiotic used for community-acquired pneumonia and sinusitis.",
                "Tablet", "500mg", "5 Tablets / Strip",
                "https://images.unsplash.com/photo-1587854692152-cbe660dbde88?w=500&auto=format&fit=crop&q=60",
                true, "LVM-4412", LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(21),
                BigDecimal.valueOf(55.00), BigDecimal.valueOf(85.00), 150);

        createMedWithBatch(antibioticsCat, "Monocef-O 200 Tablet", "Aristo", "Cefpodoxime Proxetil", "Aristo Pharmaceuticals",
                "Potent oral cephalosporin targeting stubborn ENT and lower respiratory tract bacterial strains.",
                "Tablet", "200mg", "10 Tablets / Strip",
                "https://images.unsplash.com/photo-1550572017-edd951aa8f72?w=500&auto=format&fit=crop&q=60",
                true, "MNC-6602", LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(17),
                BigDecimal.valueOf(130.00), BigDecimal.valueOf(185.00), 110);

        // Cardiovascular & Heart Health (Additional)
        createMedWithBatch(cardioCat, "Starpress XL 25", "Lupin", "Metoprolol Succinate Prolonged Release", "Lupin Pharmaceuticals",
                "Long-acting cardio-selective beta-blocker indicated for angina pectoris and heart failure stabilization.",
                "Tablet", "25mg", "15 Tablets / Strip",
                "https://images.unsplash.com/photo-1579165466791-788226ab77b6?w=500&auto=format&fit=crop&q=60",
                true, "STP-9921", LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(21),
                BigDecimal.valueOf(65.00), BigDecimal.valueOf(95.00), 170);

        createMedWithBatch(cardioCat, "Cardivas 3.125 Tablet", "Sun Pharma", "Carvedilol", "Sun Pharmaceutical Industries",
                "Alpha and beta adrenergic blocker for high blood pressure and congestive heart failure care.",
                "Tablet", "3.125mg", "10 Tablets / Strip",
                "https://images.unsplash.com/photo-1584017911766-d451b3d0e843?w=500&auto=format&fit=crop&q=60",
                true, "CRD-2209", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(22),
                BigDecimal.valueOf(38.00), BigDecimal.valueOf(56.00), 140);

        createMedWithBatch(cardioCat, "Clopilet 75 Tablet", "Sun Pharma", "Clopidogrel", "Sun Pharmaceutical Industries",
                "Platelet aggregation inhibitor preventing stroke and myocardial infarction in high-risk patients.",
                "Tablet", "75mg", "15 Tablets / Strip",
                "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=500&auto=format&fit=crop&q=60",
                true, "CLP-8811", LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(21),
                BigDecimal.valueOf(88.00), BigDecimal.valueOf(130.00), 160);

        createMedWithBatch(cardioCat, "Losar 50 Tablet", "Unichem", "Losartan Potassium", "Unichem Laboratories",
                "First-line angiotensin II antagonist for essential hypertension and stroke risk reduction.",
                "Tablet", "50mg", "15 Tablets / Strip",
                "https://images.unsplash.com/photo-1587854692152-cbe660dbde88?w=500&auto=format&fit=crop&q=60",
                true, "LSR-5520", LocalDate.now().minusMonths(4), LocalDate.now().plusMonths(20),
                BigDecimal.valueOf(92.00), BigDecimal.valueOf(138.00), 150);

        createMedWithBatch(cardioCat, "Cardace 2.5 Tablet", "Sanofi", "Ramipril", "Sanofi India",
                "ACE inhibitor helping blood vessels relax, lowering cardiac pressure and improving survival post-heart attack.",
                "Tablet", "2.5mg", "15 Tablets / Strip",
                "https://images.unsplash.com/photo-1471864190281-a93a3070b6de?w=500&auto=format&fit=crop&q=60",
                true, "CDC-3318", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(22),
                BigDecimal.valueOf(68.00), BigDecimal.valueOf(98.00), 130);

        // Diabetes Care (Additional)
        createMedWithBatch(diabetesCat, "Jardiance 10mg Tablet", "Boehringer Ingelheim", "Empagliflozin", "Boehringer Ingelheim India",
                "SGLT-2 inhibitor removing excess glucose through urine and supporting cardio-renal safety.",
                "Tablet", "10mg", "10 Tablets / Strip",
                "https://images.unsplash.com/photo-1607613009820-a29f7bb81c04?w=500&auto=format&fit=crop&q=60",
                true, "JRD-7710", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(22),
                BigDecimal.valueOf(390.00), BigDecimal.valueOf(540.00), 80);

        createMedWithBatch(diabetesCat, "Forxiga 10mg Tablet", "AstraZeneca", "Dapagliflozin", "AstraZeneca Pharma",
                "Advanced glycemic control medication that assists in weight management and heart failure defense.",
                "Tablet", "10mg", "14 Tablets / Strip",
                "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=500&auto=format&fit=crop&q=60",
                true, "FXG-4419", LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(23),
                BigDecimal.valueOf(520.00), BigDecimal.valueOf(720.00), 75);

        createMedWithBatch(diabetesCat, "Rybelsus 7mg Tablet", "Novo Nordisk", "Semaglutide Oral", "Novo Nordisk India",
                "First oral GLP-1 receptor agonist for comprehensive HbA1c lowering and weight optimization in diabetes.",
                "Tablet", "7mg", "10 Tablets / Strip",
                "https://images.unsplash.com/photo-1607613009820-a29f7bb81c04?w=500&auto=format&fit=crop&q=60",
                true, "RYB-9903", LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(20),
                BigDecimal.valueOf(2150.00), BigDecimal.valueOf(2890.00), 50);

        createMedWithBatch(diabetesCat, "Zita Plus 20mg Tablet", "Glenmark", "Teneligliptin", "Glenmark Pharmaceuticals",
                "Economical and effective DPP-4 inhibitor providing steady glycemic control with minimal hypoglycemia.",
                "Tablet", "20mg", "15 Tablets / Strip",
                "https://images.unsplash.com/photo-1587854692152-cbe660dbde88?w=500&auto=format&fit=crop&q=60",
                true, "ZTA-6601", LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(21),
                BigDecimal.valueOf(85.00), BigDecimal.valueOf(125.00), 160);

        createMedWithBatch(diabetesCat, "Janumet 50/1000 Tablet", "MSD", "Sitagliptin + Metformin HCl", "Merck Sharp & Dohme",
                "Combination therapy designed for dual-pathway glycemic control in uncontrolled diabetes patients.",
                "Tablet", "50mg + 1000mg", "15 Tablets / Strip",
                "https://images.unsplash.com/photo-1584017911766-d451b3d0e843?w=500&auto=format&fit=crop&q=60",
                true, "JNM-3312", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(22),
                BigDecimal.valueOf(330.00), BigDecimal.valueOf(460.00), 95);

        // Respiratory & Allergy (Additional)
        createMedWithBatch(respCat, "Budecort 200 Inhaler", "Cipla", "Budesonide Inhalation Aerosol", "Cipla Limited",
                "Corticosteroid maintenance inhaler preventing bronchial swelling and asthma attacks.",
                "Inhaler", "200mcg", "200 MDI Doses",
                "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=500&auto=format&fit=crop&q=60",
                true, "BDC-8820", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(22),
                BigDecimal.valueOf(260.00), BigDecimal.valueOf(365.00), 110);

        createMedWithBatch(respCat, "Foracort 200 Synchrobreathe", "Cipla", "Formoterol + Budesonide", "Cipla Limited",
                "Breath-actuated inhaler delivering rapid bronchodilation and anti-inflammatory coverage.",
                "Inhaler", "6mcg + 200mcg", "120 Actuations",
                "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=500&auto=format&fit=crop&q=60",
                true, "FRC-1109", LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(23),
                BigDecimal.valueOf(350.00), BigDecimal.valueOf(490.00), 90);

        createMedWithBatch(respCat, "Alex Sugar Free Cough Syrup", "Glenmark", "Phenylephrine + Chlorpheniramine + Dextromethorphan", "Glenmark Pharmaceuticals",
                "Diabetic-safe pleasant syrup soothing irritated throat mucosa and hacking non-productive cough.",
                "Syrup", "100ml", "100ml Bottle",
                "https://images.unsplash.com/photo-1603555501671-8f96b3fce8e4?w=500&auto=format&fit=crop&q=60",
                false, "ALX-7714", LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(21),
                BigDecimal.valueOf(88.00), BigDecimal.valueOf(125.00), 170);

        createMedWithBatch(respCat, "Cofsils Lozenges Honey Lemon", "Cipla", "Amylmetacresol + Dichlorobenzyl Alcohol", "Cipla Health",
                "Antibacterial soothing throat lozenges giving quick relief from scratchy sore throat.",
                "Lozenge", "10 Lozenges", "10 Lozenges / Strip",
                "https://images.unsplash.com/photo-1587854692152-cbe660dbde88?w=500&auto=format&fit=crop&q=60",
                false, "CFS-5502", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(28),
                BigDecimal.valueOf(22.00), BigDecimal.valueOf(35.00), 300);

        createMedWithBatch(respCat, "Levolin 50mcg Inhaler", "Cipla", "Levosalbutamol Inhalation", "Cipla Limited",
                "Pure enantiomer bronchodilator for fast airway opening with fewer cardiac palpitations.",
                "Inhaler", "50mcg", "200 Actuations",
                "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=500&auto=format&fit=crop&q=60",
                true, "LVL-3321", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(22),
                BigDecimal.valueOf(135.00), BigDecimal.valueOf(185.00), 125);

        // Gastrointestinal & Digestion (Additional)
        createMedWithBatch(gastroCat, "Rantac 150 Tablet", "JB Chemicals", "Ranitidine Hydrochloride", "JB Chemicals & Pharma",
                "Classic H2-blocker soothing stomach acid surges, sour burps, and reflux discomfort.",
                "Tablet", "150mg", "30 Tablets / Strip",
                "https://images.unsplash.com/photo-1584017911766-d451b3d0e843?w=500&auto=format&fit=crop&q=60",
                false, "RNT-9908", LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(21),
                BigDecimal.valueOf(26.00), BigDecimal.valueOf(39.50), 250);

        createMedWithBatch(gastroCat, "Razo 20 Tablet", "Dr. Reddy's", "Rabeprazole Sodium", "Dr. Reddy's Laboratories",
                "Fast-onset enteric coated PPI providing 24-hour protection against peptic ulcers and reflux.",
                "Tablet", "20mg", "15 Tablets / Strip",
                "https://images.unsplash.com/photo-1550572017-edd951aa8f72?w=500&auto=format&fit=crop&q=60",
                false, "RZO-4412", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(22),
                BigDecimal.valueOf(120.00), BigDecimal.valueOf(175.00), 180);

        createMedWithBatch(gastroCat, "Dulcolax 5mg Tablet", "Sanofi", "Bisacodyl", "Sanofi India",
                "Stimulant laxative acting overnight directly on colon muscles for dependable morning regularity.",
                "Tablet", "5mg", "10 Tablets / Strip",
                "https://images.unsplash.com/photo-1587854692152-cbe660dbde88?w=500&auto=format&fit=crop&q=60",
                false, "DLC-2210", LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(27),
                BigDecimal.valueOf(10.00), BigDecimal.valueOf(15.50), 350);

        createMedWithBatch(gastroCat, "Eno Fruit Salt Lemon 100g", "Haleon", "Svarjiksara + Nimbukamlam", "Haleon India",
                "Effervescent antacid powder acting within 6 seconds on acute acidity and heaviness.",
                "Powder", "100g", "100g Glass Bottle",
                "https://images.unsplash.com/photo-1584017911766-d451b3d0e843?w=500&auto=format&fit=crop&q=60",
                false, "ENO-6619", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(34),
                BigDecimal.valueOf(110.00), BigDecimal.valueOf(155.00), 190);

        createMedWithBatch(gastroCat, "Ondem 4mg MD Tablet", "Alkem", "Ondansetron Mouth Dissolving", "Alkem Laboratories",
                "Rapid dissolve anti-emetic stopping nausea, vomiting, and dizziness within minutes.",
                "Tablet", "4mg", "10 Tablets / Strip",
                "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=500&auto=format&fit=crop&q=60",
                false, "ODM-8831", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(22),
                BigDecimal.valueOf(35.00), BigDecimal.valueOf(52.00), 220);

        // Vitamins & Supplements (Additional)
        createMedWithBatch(vitCat, "Evion 400mg Capsule", "P&G Health", "Vitamin E (Tocopheryl Acetate)", "Procter & Gamble Health",
                "Essential antioxidant soft gelatin capsule nourishing skin cells, hair follicles, and muscular recovery.",
                "Capsule", "400mg", "10 Capsules / Strip",
                "https://images.unsplash.com/photo-1584017911766-d451b3d0e843?w=500&auto=format&fit=crop&q=60",
                false, "EVN-7704", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(26),
                BigDecimal.valueOf(24.00), BigDecimal.valueOf(35.00), 320);

        createMedWithBatch(vitCat, "Supradyn Daily Multivitamin", "Bayer", "12 Vitamins + 5 Minerals + Trace Elements", "Bayer Pharmaceuticals",
                "Balanced daily multivitamin for overall stamina, energy boost, and cell regeneration.",
                "Tablet", "Daily Formula", "15 Tablets / Strip",
                "https://images.unsplash.com/photo-1587854692152-cbe660dbde88?w=500&auto=format&fit=crop&q=60",
                false, "SPN-3310", LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(21),
                BigDecimal.valueOf(42.00), BigDecimal.valueOf(59.00), 260);

        createMedWithBatch(vitCat, "Feronia XT Tablet", "Zuventus", "Ferrous Ascorbate + Folic Acid", "Zuventus Healthcare",
                "Well-tolerated elemental iron supplement restoring hemoglobin levels in anemia and pregnancy.",
                "Tablet", "100mg + 1.5mg", "10 Tablets / Strip",
                "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=500&auto=format&fit=crop&q=60",
                false, "FRN-9912", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(22),
                BigDecimal.valueOf(125.00), BigDecimal.valueOf(178.00), 160);

        createMedWithBatch(vitCat, "Seven Seas Original Cod Liver Oil", "P&G Health", "Pure Cod Liver Oil with Vitamin A & D", "Procter & Gamble Health",
                "Rich source of natural Omega-3 fatty acids EPA & DHA for joint flexibility, heart, and brain.",
                "Capsule", "500mg", "100 Capsules Bottle",
                "https://images.unsplash.com/photo-1584017911766-d451b3d0e843?w=500&auto=format&fit=crop&q=60",
                false, "SSO-1120", LocalDate.now().minusMonths(4), LocalDate.now().plusMonths(20),
                BigDecimal.valueOf(280.00), BigDecimal.valueOf(390.00), 110);

        createMedWithBatch(vitCat, "Celin 500 Chewable Tablet", "Abbott", "Ascorbic Acid (Vitamin C)", "Abbott India",
                "Tangy chewable immunity tablets that support collagen synthesis, wound healing, and iron uptake.",
                "Tablet", "500mg", "25 Tablets / Strip",
                "https://images.unsplash.com/photo-1587854692152-cbe660dbde88?w=500&auto=format&fit=crop&q=60",
                false, "CLN-5509", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(22),
                BigDecimal.valueOf(28.00), BigDecimal.valueOf(39.50), 290);

        // Dermatology & Skin Care (Additional)
        createMedWithBatch(skinCat, "Betnovate-C Cream", "GlaxoSmithKline", "Betamethasone Valerate + Clioquinol", "GlaxoSmithKline",
                "Topical corticosteroid with antifungal/antibacterial agent for eczema, dermatitis, and fungal flares.",
                "Cream", "20g Tube", "20g Tube",
                "https://images.unsplash.com/photo-1556228720-195a672e8a03?w=500&auto=format&fit=crop&q=60",
                true, "BNC-6612", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(22),
                BigDecimal.valueOf(44.00), BigDecimal.valueOf(62.00), 200);

        createMedWithBatch(skinCat, "Candid-B Cream", "Glenmark", "Clotrimazole + Beclomethasone", "Glenmark Pharmaceuticals",
                "Dual action anti-inflammatory and antifungal cream for ringworm, athlete's foot, and jock itch.",
                "Cream", "20g Tube", "20g Tube",
                "https://images.unsplash.com/photo-1556228720-195a672e8a03?w=500&auto=format&fit=crop&q=60",
                false, "CDB-8819", LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(21),
                BigDecimal.valueOf(98.00), BigDecimal.valueOf(142.00), 180);

        createMedWithBatch(skinCat, "Clindac-A Foaming Gel", "Alkem", "Clindamycin Phosphate 1%", "Alkem Laboratories",
                "Targeted topical antibiotic gel effectively clearing inflammatory acne lesions and pimples.",
                "Gel", "1% w/w", "20g Tube",
                "https://images.unsplash.com/photo-1556228720-195a672e8a03?w=500&auto=format&fit=crop&q=60",
                true, "CLA-3310", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(22),
                BigDecimal.valueOf(160.00), BigDecimal.valueOf(230.00), 150);

        createMedWithBatch(skinCat, "Nizral 2% Antidandruff Solution", "Janssen", "Ketoconazole 2%", "Johnson & Johnson India",
                "Clinically proven medical shampoo eliminating stubborn dandruff, flaky scalp, and seborrheic dermatitis.",
                "Liquid", "2% w/v", "50ml Bottle",
                "https://images.unsplash.com/photo-1603555501671-8f96b3fce8e4?w=500&auto=format&fit=crop&q=60",
                false, "NZR-5501", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(22),
                BigDecimal.valueOf(195.00), BigDecimal.valueOf(280.00), 120);

        createMedWithBatch(skinCat, "Sebogel Salicylic Gel", "Curatio", "Salicylic Acid 2% + Nicotinamide 6%", "Curatio Healthcare",
                "Dermatologist-recommended non-comedogenic gel clearing blackheads and opening clogged pores.",
                "Gel", "30g", "30g Tube",
                "https://images.unsplash.com/photo-1556228720-195a672e8a03?w=500&auto=format&fit=crop&q=60",
                false, "SBG-2219", LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(23),
                BigDecimal.valueOf(170.00), BigDecimal.valueOf(240.00), 140);

        // First Aid & Devices (Additional)
        createMedWithBatch(firstAidCat, "Savlon Antiseptic Liquid", "ITC", "Chlorhexidine Gluconate + Cetrimide", "ITC Limited",
                "Gentle sting-free antiseptic liquid effective against 99.9% of germs on cuts, scrapes, and skin wounds.",
                "Liquid", "200ml", "200ml Bottle",
                "https://images.unsplash.com/photo-1603555501671-8f96b3fce8e4?w=500&auto=format&fit=crop&q=60",
                false, "SVL-7712", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(34),
                BigDecimal.valueOf(75.00), BigDecimal.valueOf(108.00), 190);

        createMedWithBatch(firstAidCat, "Volini Pain Relief Spray", "Sun Pharma", "Diclofenac Diethylamine + Menthol", "Sun Pharmaceutical Industries",
                "Instant micro-droplet pain relief spray penetrating deep to ease muscular spasms and athletic sprains.",
                "Spray", "100g Spray", "100g Canister",
                "https://images.unsplash.com/photo-1556228720-195a672e8a03?w=500&auto=format&fit=crop&q=60",
                false, "VLS-4409", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(30),
                BigDecimal.valueOf(185.00), BigDecimal.valueOf(255.00), 130);

        createMedWithBatch(firstAidCat, "Dr. Trust Finger Pulse Oximeter", "Dr. Trust", "SpO2 & Pulse Rate Optical Sensor", "Nureca Inc / Dr. Trust",
                "Medical-grade non-invasive pulse oximeter with multi-directional color OLED display.",
                "Device", "1 Unit", "1 Device Kit",
                "https://images.unsplash.com/photo-1584515979956-d9f6e5d09982?w=500&auto=format&fit=crop&q=60",
                false, "DTO-1120", LocalDate.now().minusMonths(5), LocalDate.now().plusMonths(48),
                BigDecimal.valueOf(799.00), BigDecimal.valueOf(1199.00), 60);

        createMedWithBatch(firstAidCat, "Omron Digital Blood Pressure Monitor", "Omron", "Intellisense Oscillometric Sensor", "Omron Healthcare India",
                "Fully automatic upper-arm blood pressure monitor with hypertension indicator and memory.",
                "Device", "1 Unit", "1 Unit Box",
                "https://images.unsplash.com/photo-1584515979956-d9f6e5d09982?w=500&auto=format&fit=crop&q=60",
                false, "OMR-8810", LocalDate.now().minusMonths(6), LocalDate.now().plusMonths(48),
                BigDecimal.valueOf(1650.00), BigDecimal.valueOf(2250.00), 45);

        createMedWithBatch(firstAidCat, "Cotton Crepe Bandage 10cm", "Dynamic", "Elastic Cotton Webbing", "Dynamic Techno Medicals",
                "Breathable elastic crepe bandage providing uniform compression for sprained joints and strains.",
                "Device", "10cm x 4m", "Single Roll Box",
                "https://images.unsplash.com/photo-1584515979956-d9f6e5d09982?w=500&auto=format&fit=crop&q=60",
                false, "CPB-5521", LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(36),
                BigDecimal.valueOf(90.00), BigDecimal.valueOf(135.00), 180);

        // Eye, Ear & Oral Care
        createMedWithBatch(eyeOralCat, "Refresh Tears Lubricant Eye Drops", "Allergan", "Carboxymethylcellulose 0.5%", "Allergan India",
                "Restorative artificial tears providing soothing lubrication for dry, irritated, screen-tired eyes.",
                "Liquid", "0.5% w/v", "10ml Sterile Dropper",
                "https://images.unsplash.com/photo-1603555501671-8f96b3fce8e4?w=500&auto=format&fit=crop&q=60",
                false, "RFT-6622", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(22),
                BigDecimal.valueOf(115.00), BigDecimal.valueOf(165.00), 170);

        createMedWithBatch(eyeOralCat, "Waxsol Ear Drops", "Abbott", "Docusate Sodium 0.5%", "Abbott Healthcare",
                "Water-miscible cerumenolytic ear drops gently softening hardened ear wax for easy removal.",
                "Liquid", "0.5% w/v", "10ml Dropper Bottle",
                "https://images.unsplash.com/photo-1603555501671-8f96b3fce8e4?w=500&auto=format&fit=crop&q=60",
                false, "WXS-3301", LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(21),
                BigDecimal.valueOf(85.00), BigDecimal.valueOf(125.00), 140);

        createMedWithBatch(eyeOralCat, "Sensodyne Rapid Relief Toothpaste", "GSK", "Strontium Acetate Formulation", "GlaxoSmithKline Consumer",
                "Clinically proven desensitizing oral toothpaste relieving painful tooth sensitivity in 60 seconds.",
                "Cream", "80g Tube", "80g Tube",
                "https://images.unsplash.com/photo-1556228720-195a672e8a03?w=500&auto=format&fit=crop&q=60",
                false, "SNS-9914", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(24),
                BigDecimal.valueOf(145.00), BigDecimal.valueOf(200.00), 160);

        createMedWithBatch(eyeOralCat, "Betadine Gargle 2% Mint", "Win-Medicare", "Povidone-Iodine 2%", "Win-Medicare Pvt Ltd",
                "Germicidal oral mouthwash and gargle for throat infections, pharyngitis, and mouth ulcers.",
                "Liquid", "2% w/v", "100ml Bottle",
                "https://images.unsplash.com/photo-1603555501671-8f96b3fce8e4?w=500&auto=format&fit=crop&q=60",
                false, "BTG-4402", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(22),
                BigDecimal.valueOf(110.00), BigDecimal.valueOf(158.00), 150);

        logger.info("Successfully populated extensive catalogue of 95+ real medicines with active FEFO inventory batches!");
    }

    private MedicineCategory getOrCreateCategory(String name, String desc) {
        return categoryRepository.findByName(name).orElseGet(() ->
                categoryRepository.save(MedicineCategory.builder()
                        .name(name)
                        .description(desc)
                        .active(true)
                        .build())
        );
    }

    private void createMedWithBatch(MedicineCategory category, String name, String brand, String genericName,
                                   String manufacturer, String description, String dosageForm, String strength,
                                   String packSize, String imageUrl, boolean requiresPrescription,
                                   String batchNumber, LocalDate mfgDate, LocalDate expDate,
                                   BigDecimal purchasePrice, BigDecimal sellingPrice, int qty) {
        if (medicineRepository.existsByName(name)) {
            return;
        }

        Medicine med = medicineRepository.save(Medicine.builder()
                .category(category)
                .name(name)
                .brand(brand)
                .genericName(genericName)
                .manufacturer(manufacturer)
                .description(description)
                .dosageForm(dosageForm)
                .strength(strength)
                .packSize(packSize)
                .productImageUrl(imageUrl)
                .requiresPrescription(requiresPrescription)
                .active(true)
                .build());

        MedicineBatch batch = batchRepository.save(MedicineBatch.builder()
                .medicine(med)
                .batchNumber(batchNumber)
                .manufacturingDate(mfgDate)
                .expiryDate(expDate)
                .purchasePrice(purchasePrice)
                .sellingPrice(sellingPrice)
                .quantityReceived(qty)
                .availableQuantity(qty)
                .build());

        movementRepository.save(StockMovement.builder()
                .batch(batch)
                .movementType(StockMovementType.STOCK_RECEIVED)
                .quantity(qty)
                .referenceType("INITIAL_SEED")
                .referenceId("SEED-001")
                .notes("Seed inventory batch")
                .createdByUserId(1L)
                .build());
    }
}

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TestOrders {
    public static void main(String[] args) {
        System.out.println("=== DÉBUT DES TESTS DE GESTION DES COMMANDES ===\n");

        DataRetriever dataRetriever = new DataRetriever();

        try {
            // Étape 1: Nettoyer et préparer la base de données
            System.out.println("1. Préparation de la base de données...");
            prepareDatabase();

            // Étape 2: Vérifier les données existantes
            System.out.println("\n2. Vérification des données existantes...");
            displayCurrentData(dataRetriever);

            // Étape 3: Créer et sauvegarder une commande
            System.out.println("\n3. Test: Création d'une commande...");
            testCreateOrderSuccess(dataRetriever);

            // Étape 4: Tester la vérification des stocks insuffisants
            System.out.println("\n4. Test: Vérification des stocks insuffisants...");
            testInsufficientStock(dataRetriever);

            // Étape 5: Tester la recherche de commande
            System.out.println("\n5. Test: Recherche de commande...");
            testFindOrder(dataRetriever);

            // Étape 6: Tester les calculs de montants
            System.out.println("\n6. Test: Calculs des montants...");
            testAmountCalculations(dataRetriever);

            System.out.println("\n=== TOUS LES TESTS SONT TERMINÉS ===");

        } catch (Exception e) {
            System.err.println("Erreur pendant les tests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void prepareDatabase() throws SQLException {
        DBConnection dbConnection = new DBConnection();
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Désactiver les contraintes temporairement
            stmt.execute("SET session_replication_role = 'replica';");

            // Nettoyer les tables (attention en production !)
            stmt.execute("DELETE FROM dish_order;");
            stmt.execute("DELETE FROM \"order\";");
            stmt.execute("DELETE FROM stock_movement;");
            stmt.execute("DELETE FROM dish_ingredient;");
            stmt.execute("DELETE FROM ingredient;");
            stmt.execute("DELETE FROM dish;");

            // Réactiver les contraintes
            stmt.execute("SET session_replication_role = 'origin';");

            // Insérer des données de test
            insertTestData(conn);

            System.out.println("✅ Base de données préparée avec succès");
        }
    }

    private static void insertTestData(Connection conn) throws SQLException {
        // Insérer des plats
        String insertDish = """
            INSERT INTO dish (id, name, dish_type, selling_price) VALUES 
            (1, 'Salade fraîche', 'STARTER', 3500.00),
            (2, 'Poulet grillé', 'MAIN', 12000.00),
            (3, 'Gâteau au chocolat', 'DESSERT', 8000.00)
            ON CONFLICT (id) DO NOTHING;
        """;

        // Insérer des ingrédients
        String insertIngredient = """
            INSERT INTO ingredient (id, name, category, price) VALUES 
            (1, 'Laitue', 'VEGETABLE', 800.00),
            (2, 'Tomate', 'VEGETABLE', 600.00),
            (3, 'Poulet', 'ANIMAL', 4500.00),
            (4, 'Chocolat', 'OTHER', 3000.00),
            (5, 'Beurre', 'DAIRY', 2500.00)
            ON CONFLICT (id) DO NOTHING;
        """;

        // Insérer les associations plat-ingrédient
        String insertDishIngredient = """
            INSERT INTO dish_ingredient (dish_id, ingredient_id, quantity_required, unit) VALUES 
            (1, 1, 0.20, 'KG'),
            (1, 2, 0.15, 'KG'),
            (2, 3, 1.00, 'KG'),
            (3, 4, 0.30, 'KG'),
            (3, 5, 0.20, 'KG')
            ON CONFLICT (dish_id, ingredient_id) DO NOTHING;
        """;

        // Insérer des mouvements de stock initiaux
        String insertStockMovement = """
            INSERT INTO stock_movement (id_ingredient, quantity, type, unit, creation_datetime) VALUES 
            (1, 100.0, 'IN', 'KG', NOW() - INTERVAL '1 day'),
            (2, 100.0, 'IN', 'KG', NOW() - INTERVAL '1 day'),
            (3, 50.0, 'IN', 'KG', NOW() - INTERVAL '1 day'),
            (4, 20.0, 'IN', 'KG', NOW() - INTERVAL '1 day'),
            (5, 15.0, 'IN', 'KG', NOW() - INTERVAL '1 day')
            ON CONFLICT DO NOTHING;
        """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(insertDish);
            stmt.execute(insertIngredient);
            stmt.execute(insertDishIngredient);
            stmt.execute(insertStockMovement);
        }
    }

    private static void displayCurrentData(DataRetriever dataRetriever) {
        try {
            // Afficher les plats
            System.out.println("\n📋 Plats disponibles:");
            for (int i = 1; i <= 3; i++) {
                Dish dish = dataRetriever.findDishById(i);
                System.out.printf("  - %s (ID: %d) - Prix: %.2f FCFA - Coût: %.2f FCFA%n",
                        dish.getName(), dish.getId(), dish.getPrice(), dish.getDishCost());

                System.out.println("    Ingrédients:");
                for (DishIngredient di : dish.getDishIngredients()) {
                    System.out.printf("      • %s: %.2f %s (%.2f FCFA)%n",
                            di.getIngredient().getName(), di.getQuantity(),
                            di.getUnit(), di.getIngredient().getPrice());
                }
            }

            // Afficher les stocks
            System.out.println("\n📊 Stocks actuels:");
            for (int i = 1; i <= 5; i++) {
                Ingredient ingredient = dataRetriever.findIngredientById(i);
                System.out.printf("  - %s: %.2f %s%n",
                        ingredient.getName(),
                        ingredient.getStockValueAt(Instant.now()).getQuantity(),
                        ingredient.getStockValueAt(Instant.now()).getUnit());
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'affichage des données: " + e.getMessage());
        }
    }

    private static void testCreateOrderSuccess(DataRetriever dataRetriever) {
        try {
            // Créer une commande valide
            Order order = new Order();
            order.setCreationDatetime(Instant.now());

            // Créer les plats commandés
            List<DishOrder> dishOrders = new ArrayList<>();

            DishOrder dishOrder1 = new DishOrder();
            Dish dish1 = dataRetriever.findDishById(1); // Salade fraîche
            dishOrder1.setDish(dish1);
            dishOrder1.setQuantity(2); // 2 salades
            dishOrders.add(dishOrder1);

            DishOrder dishOrder2 = new DishOrder();
            Dish dish2 = dataRetriever.findDishById(3); // Gâteau au chocolat
            dishOrder2.setDish(dish2);
            dishOrder2.setQuantity(1); // 1 gâteau
            dishOrders.add(dishOrder2);

            order.setDishOrderList(dishOrders);

            // Sauvegarder la commande
            Order savedOrder = dataRetriever.saveOrder(order);

            System.out.println("✅ Commande créée avec succès!");
            System.out.println("   Référence: " + savedOrder.getReference());
            System.out.println("   Date: " + savedOrder.getCreationDatetime());
            System.out.println("   Plats commandés:");
            for (DishOrder do : savedOrder.getDishOrderList()) {
                System.out.printf("     - %s x%d (%.2f FCFA pièce)%n",
                do.getDish().getName(), do.getQuantity(), do.getDish().getPrice());
            }

            // Vérifier que les stocks ont été déduits
            System.out.println("\n   Vérification des stocks après commande:");
            Ingredient laitue = dataRetriever.findIngredientById(1);
            double laitueStock = laitue.getStockValueAt(Instant.now()).getQuantity();
            System.out.printf("   - Laitue: %.2f KG (devrait être ~99.6 KG)%n", laitueStock);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la création de commande: " + e.getMessage());
        }
    }

    private static void testInsufficientStock(DataRetriever dataRetriever) {
        try {
            // Créer une commande qui nécessite plus de stock que disponible
            Order order = new Order();
            order.setCreationDatetime(Instant.now());

            List<DishOrder> dishOrders = new ArrayList<>();

            DishOrder dishOrder = new DishOrder();
            Dish dish = dataRetriever.findDishById(2); // Poulet grillé
            dishOrder.setDish(dish);
            dishOrder.setQuantity(100); // 100 poulets grillés (nécessite 100 KG de poulet)
            dishOrders.add(dishOrder);

            order.setDishOrderList(dishOrders);

            // Cette commande devrait échouer
            dataRetriever.saveOrder(order);

            System.err.println("❌ TEST ÉCHOUÉ: La commande aurait dû échouer par manque de stock!");

        } catch (RuntimeException e) {
            if (e.getMessage().contains("Stock insuffisant")) {
                System.out.println("✅ Test de vérification des stocks réussi!");
                System.out.println("   Message d'erreur: " + e.getMessage());
            } else {
                System.err.println("❌ Mauvaise exception: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur inattendue: " + e.getMessage());
        }
    }

    private static void testFindOrder(DataRetriever dataRetriever) {
        try {
            // Créer une commande pour tester la recherche
            Order order = new Order();
            order.setCreationDatetime(Instant.now());

            List<DishOrder> dishOrders = new ArrayList<>();
            DishOrder dishOrder = new DishOrder();
            dishOrder.setDish(dataRetriever.findDishById(1));
            dishOrder.setQuantity(1);
            dishOrders.add(dishOrder);
            order.setDishOrderList(dishOrders);

            Order savedOrder = dataRetriever.saveOrder(order);
            String reference = savedOrder.getReference();

            // Rechercher la commande par référence
            Order foundOrder = dataRetriever.findOrderByReference(reference);

            if (foundOrder != null && foundOrder.getReference().equals(reference)) {
                System.out.println("✅ Recherche de commande réussie!");
                System.out.println("   Référence trouvée: " + foundOrder.getReference());
                System.out.println("   Nombre de plats: " + foundOrder.getDishOrderList().size());
            } else {
                System.err.println("❌ La commande n'a pas été trouvée");
            }

            // Tester avec une référence inexistante
            try {
                dataRetriever.findOrderByReference("ORD99999");
                System.err.println("❌ L'exception n'a pas été levée pour une référence inexistante");
            } catch (RuntimeException e) {
                if (e.getMessage().contains("Order not found")) {
                    System.out.println("✅ Exception levée correctement pour référence inexistante");
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du test de recherche: " + e.getMessage());
        }
    }

    private static void testAmountCalculations(DataRetriever dataRetriever) {
        try {
            // Créer une méthode de test pour Order.getTotalAmountWithoutVat()
            // Note: Tu dois d'abord implémenter cette méthode dans Order.java

            Order order = new Order();

            // Créer des DishOrder de test
            List<DishOrder> dishOrders = new ArrayList<>();

            DishOrder do1 = new DishOrder();
            Dish d1 = dataRetriever.findDishById(1); // 3500 FCFA
            do1.setDish(d1);
            do1.setQuantity(2); // 2 × 3500 = 7000

            DishOrder do2 = new DishOrder();
            Dish d2 = dataRetriever.findDishById(3); // 8000 FCFA
            do2.setDish(d2);
            do2.setQuantity(1); // 1 × 8000 = 8000

            dishOrders.add(do1);
            dishOrders.add(do2);
            order.setDishOrderList(dishOrders);

            // Calculer le total attendu
            double expectedTotal = (3500.00 * 2) + (8000.00 * 1); // 7000 + 8000 = 15000

            System.out.println("💰 Test des calculs de montants:");
            System.out.println("   Total attendu HT: " + expectedTotal + " FCFA");

            // Si tu as implémenté la méthode:
            // double actualTotal = order.getTotalAmountWithoutVat();
            // System.out.println("   Total calculé HT: " + actualTotal + " FCFA");

            // if (Math.abs(actualTotal - expectedTotal) < 0.01) {
            //     System.out.println("✅ Calcul du montant HT réussi!");
            // } else {
            //     System.err.println("❌ Calcul incorrect");
            // }

            System.out.println("   ⚠️ Implémente Order.getTotalAmountWithoutVat() pour tester");

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du test des calculs: " + e.getMessage());
        }
    }
}
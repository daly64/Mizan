package tn.mizan.app.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Catégories de dépenses (stockées par leur nom dans Room). */
enum class Category(val label: String, val icon: ImageVector) {
    COURSES("Courses", Icons.Outlined.ShoppingCart),
    TRANSPORT("Transport", Icons.Outlined.DirectionsBus),
    LOISIRS("Loisirs", Icons.Outlined.ConfirmationNumber),
    FACTURES("Factures", Icons.AutoMirrored.Outlined.ReceiptLong)
}

enum class PaymentMethod(val label: String) {
    ESPECES("Espèces"),
    CARTE("Carte")
}

/**
 * Une dépense ou un revenu.
 * Les montants sont en millimes (1 dinar = 1000 millimes) pour éviter les erreurs d'arrondi.
 */
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountMillimes: Long,
    val category: Category,
    val note: String,
    val dateMillis: Long,
    val payment: PaymentMethod,
    val isIncome: Boolean = false
)

/** Budget mensuel d'une catégorie, en millimes. */
@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey val category: Category,
    val limitMillimes: Long
)

/** Note libre prise par l'assistant (ou plus tard à la main). */
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val dateMillis: Long
)

// Please see documentation at https://docs.microsoft.com/aspnet/core/client-side/bundling-and-minification
// for details on configuring this project to bundle and minify static web assets.

/* Initialization for ES Users Navbar */
var Collapse = mdb.Collapse;
var Ripple = mdb.Ripple;
var initMDB = mdb.initMDB;

// Inizializza MDB utilizzando la sintassi ES5
initMDB({ Collapse: Collapse, Ripple: Ripple });

$(function () {
  moment.locale('it');

  // Configurazione icone per datetimepicker
  $.extend(true, $.fn.datetimepicker.defaults, {
    icons: {
      time: 'far fa-clock',
      date: 'far fa-calendar',
      up: 'fas fa-arrow-up',
      down: 'fas fa-arrow-down',
      previous: 'fas fa-chevron-left',
      next: 'fas fa-chevron-right',
      today: 'far fa-calendar-check-o',
      clear: 'far fa-trash',
      close: 'far fa-times'
    }
  });

  var currentDateTime = moment().format('YYYY-MM-DD HH:mm:ss');
  var minDate = moment.parseZone('2024-01-01 00:00:00', 'YYYY-MM-DD HH:mm:ss');
  var maxDate = moment.parseZone('2024-12-31 23:59:59', 'YYYY-MM-DD HH:mm:ss');

  // Inizializza datetimepicker
  $('#datetimepicker-ingresso, #datetimepicker-uscita').datetimepicker({
    format: 'DD-MM-YYYY HH:mm',
    useCurrent: false,
    minDate: minDate,
    maxDate: maxDate,
    ignoreReadonly: true,
    widgetPositioning: {
      horizontal: 'auto',
      vertical: 'bottom'
    },
    sideBySide: true,
    tooltips: {
      close: 'Chiudi',
      selectTime: 'Seleziona l\'orario',
      selectDate: 'Seleziona la data'
    }
  });

  // Inizializza datetimepicker per la ricerca senza minDate
  $('#datetimepicker-ingresso-search, #datetimepicker-uscita-search').datetimepicker({
    format: 'DD-MM-YYYY HH:mm',
    useCurrent: false,
    maxDate: maxDate,
    ignoreReadonly: true,
    widgetPositioning: {
      horizontal: 'auto',
      vertical: 'bottom'
    },
    sideBySide: true,
    tooltips: {
      close: 'Chiudi',
      selectTime: 'Seleziona l\'orario',
      selectDate: 'Seleziona la data'
    }
  });

  // Funzione per ottenere la data e l'ora corrente
  function getCurrentDateTime() {
    return moment().format('DD-MM-YYYY HH:mm'); // Formato dell'orario desiderato
  }

  // Funzione per aggiornare l'input con la data e l'ora corrente
  function updateDateTimeInput() {
    var currentDateTime = getCurrentDateTime();
    $('#datetimepicker-ingresso input, #datetimepicker-uscita input').val(currentDateTime); // Aggiorna il valore dell'input dell'orario
    $('#datetimepicker-ingresso-search input, #datetimepicker-uscita-search input').val(currentDateTime);
  }

  // Aggiornamento iniziale dell'orario
  updateDateTimeInput();

  // Aggiorna il valore dell'input con il formato desiderato quando si apre il calendario
  $('#datetimepicker-ingresso, #datetimepicker-uscita').on('dp.show', function () {
    var formattedDate = moment($(this).find('input').val(), 'DD-MM-YYYY HH:mm').format('DD-MM-YYYY HH:mm');
    $(this).find('input').val(formattedDate);
    $('.picker-switch.picker-switch-button').hide();
    $('.picker-switch.picker-switch-value').text('');
  });

  // Aggiorna l'opzione minDate del datetimepicker uscita quando cambia ingresso
  $('#datetimepicker-ingresso').on('dp.change', function (e) {
    if (e.date) {
      var formattedDate = e.date.format('DD-MM-YYYY HH:mm');
      $(this).find('input').val(formattedDate);
      $('#datetimepicker-uscita').datetimepicker('minDate', e.date);
      $('#datetimepicker-uscita').find('input').val(e.date.format('DD-MM-YYYY HH:mm'));
    }
  });
});

/* Aggiornare l'orario di prenotazione senza ricarica la pagina */
// Funzione per ottenere l'orario corrente formattato
function getCurrentDateTime() {
  var currentDateTime = moment().format('DD-MM-YYYY HH:mm'); // Formato dell'orario desiderato
  return currentDateTime;
}

/* Per selezionaare tipo utente in fase di SignIn */
function selectCard(card) {
  var cards = document.querySelectorAll('.card-selectable');
  cards.forEach(function (c) {
      c.classList.remove('selected');
      c.querySelector('.card-radio').checked = false;
  });
  card.classList.add('selected');
  card.querySelector('.card-radio').checked = true;
}

/* Per generare pagina .cshtml tra le opzioni per l'utente loggato */
function redirectToPage(card) {
  const pageURL = card.getAttribute('data-url');
  window.location.href = pageURL; // Aggiunge l'estensione .html all'URL
}

function isValidUsername(input) {
  let username = input.value;

  let isValid = username.length >= 8 && /^[a-zA-Z]/.test(username.charAt(0) && /^[a-zA-Z0-9]*$/.test(username))

  input.setCustomValidity(isValid ? '' : "Deve essere lungo almeno 8 caratteri, deve cominiciare con una lettera e non deve contentere caratteri speciali");

  let errorSpan = document.getElementById("usernameError");
  errorSpan.textContent = input.validationMessage;

  return isValid;
}

function isValidPassword(input) {
  let password = input.value;

  // Esegue i controlli sui criteri di complessità della password
  let hasUppercase = /[A-Z]/.test(password);
  let hasLowercase = /[a-z]/.test(password);
  let hasDigit = /\d/.test(password);
  let hasSpecialChar = /[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]/.test(password);

  // Verifica se tutti i criteri sono soddisfatti
  let isValid = password.length >= 8 && hasUppercase && hasLowercase && hasDigit && hasSpecialChar;

  // Aggiorna la validità del campo
  input.setCustomValidity(isValid ? '' : 'Deve essere lunga almeno 8 caratteri, contenere almeno una lettera maiuscola, una lettera minuscola, una cifra e un carattere speciale.');

  // Mostra il messaggio di errore sotto l'input
  let errorSpan = document.getElementById("passwordError");
  errorSpan.textContent = input.validationMessage;

  return isValid;
}

function isValidFormatAndCardNumber(input) {
  // Rimuove gli spazi e tutti i caratteri non numerici durante l'input
  let inputValue = input.value.replace(/\s/g, '').replace(/\D/g, '');

  // Formatta aggiungendo uno spazio dopo ogni gruppo di quattro cifre
  let formattedValue = inputValue.replace(/(\d{4})(?=\d)/g, '$1 ');

  // Limita il numero di cifre a 16
  formattedValue = formattedValue.substring(0, 19); // 16 cifre + 3 spazi
  
  // Aggiorna il valore nell'input
  input.value = formattedValue;

  let isValid = formattedValue.replace(/\s/g, '').length === 16;

  // Imposta la convalidità del campo in base al numero di cifre effettive, inclusi gli spazi
  input.setCustomValidity(isValid ? '' : 'Deve essere composto da 16 cifre');

  // Mostra il messaggio di errore sotto l'input
  let errorSpan = document.getElementById("numberCardError");
  errorSpan.textContent = input.validationMessage;

  return isValid;
}

function isValidFormatAndDate(input) {
  let inputValue = input.value.replace(/\D/g, ''); // Rimuove tutti i caratteri non numerici

  // Formatta aggiungendo la "/" dopo i primi due caratteri
  let formattedValue = inputValue.replace(/^(\d{2})(\d{0,2})/, '$1/$2');

  // Limita il numero di caratteri a 5 (MM/YY)
  formattedValue = formattedValue.substring(0, 5);

  // Aggiorna il valore nell'input
  input.value = formattedValue;

  // Verifica la validità del formato MM/YY e della data di scadenza
  let isValid = /^(0[1-9]|1[0-2])\/\d{2}$/.test(formattedValue) && isValidExpiryDate(formattedValue);

  // Imposta la convalidità del campo in base al formato MM/YY
  let errorSpan = document.getElementById("expiryDateError");
  if (isValid) {
    input.setCustomValidity('');
    errorSpan.textContent = '';
  } else {
    input.setCustomValidity('Inserisci una data di scadenza valida (MM/YY)');
    errorSpan.textContent = 'Inserisci una data di scadenza valida (MM/YY)';
  }

  // Controlla se il cursore è posizionato subito dopo il carattere '/' e, in tal caso, elimina anche il carattere '/'
  if (input.selectionStart === 3 && input.selectionEnd === 3 && formattedValue.charAt(2) === '/') {
    input.value = formattedValue.substring(0, 2);
  }

  // Ritorna true se la data di scadenza è valida, altrimenti false
  return isValid;
}

// Funzione per verificare se la data di scadenza è valida
function isValidExpiryDate(dateString) {
  // Estrae il mese e l'anno dalla stringa
  let month = parseInt(dateString.substring(0, 2), 10);
  let year = parseInt(dateString.substring(3), 10); // Ignora la '/'

  // Ottiene l'anno corrente (ultimi due numeri)
  let currentYear = new Date().getFullYear() % 100;

  // Controlla se il mese è valido (01-12) e se l'anno è futuro o nell'anno corrente
  return (month >= 1 && month <= 12) && (year > currentYear || (year === currentYear && month >= new Date().getMonth() + 1));
}

function isValidFormatCVV(input) {
  let inputValue = input.value;
  
  // Rimuovi tutti i caratteri non numerici
  inputValue = inputValue.replace(/\D/g, '');
  
  // Aggiorna il valore nell'input
  input.value = inputValue;

  // Verifica la validità del CVV (deve essere composto da 3 cifre)
  let isValid = inputValue.length === 3;
  
  // Imposta la convalidità del campo in base al numero di cifre effettive, inclusi gli spazi
  input.setCustomValidity(isValid ? '' : 'Deve essere composto da 3 cifre');

  // Mostra il messaggio di errore sotto l'input
  let errorSpan = document.getElementById("cvvError");
  errorSpan.textContent = input.validationMessage;

  // Ritorna true se il CVV è valido, altrimenti false
  return isValid;
}

function isValidFormatRicarica(input) {
  var value = input.value;
  var min = 10;
  var max = 100;

  // Rimuovi tutti i caratteri non numerici
  value = value.replace(/\D/g, '');

  // Rimuovi il primo zero se presente
  if (value.length > 1 && value.startsWith('0')) {
    value = value.substring(1);
  }

  // Aggiorna il valore nell'input
  input.value = value;

  let isValid = parseInt(value) >= min && parseInt(value) <= max;

  input.setCustomValidity(isValid ? '' : "Inserisci un valore tra " + min + " e " + max);

  let errorSpan = document.getElementById("RicaricaError");
  errorSpan.textContent = input.validationMessage;

  return isValid;
}

function isValidFormatPrices(input, i) {
  let inputValue = input.value;
  
  // Rimuovi tutti i caratteri non numerici, tranne il punto (.)
  inputValue = inputValue.replace(/[^\d\.]/g, '');

  // Rimuovi il primo zero se non è seguito da un punto e se non è l'unica cifra
  if (inputValue.length > 1 && inputValue.startsWith('0') && inputValue[1] !== '.') {
    inputValue = inputValue.slice(1);
  }

  // Limita a due cifre dopo il punto
  if (inputValue.includes('.')) {
    let parts = inputValue.split('.');
    parts[1] = parts[1].slice(0, 2); // Prendi solo le prime due cifre decimali
    inputValue = parts.join('.');
  }
  
  // Aggiorna il valore nell'input
  input.value = inputValue;
  
  let isValid = true;
  
  if (inputValue === '') {
    isValid = false;
  } else if (inputValue.indexOf('.') !== inputValue.lastIndexOf('.')) {
    isValid = false;
  } else if (inputValue.startsWith('.') || inputValue.endsWith('.')) {
    isValid = false;
  }
  
  input.setCustomValidity(isValid ? '' : 'Prezzo non valido');
  
  if (i == 0) {
    let errorSpan = document.getElementById("pricesErrorSosta");
    errorSpan.textContent = input.validationMessage;
  } else if (i == 1) {
    let errorSpan = document.getElementById("pricesErrorRicarica");
    errorSpan.textContent = input.validationMessage;
  }
  
  return isValid;
}

$(document).ready(function () {
  // Controlla se c'è un messaggio di errore e mostra il modal
  var errorMessage = $('#errorMessageBody').text();
  if (errorMessage.trim().length > 0) {
      $('#errorModal').modal('show');
  }
});

// Inizializzazione di Bootstrap per la validazione del form
(function () {
  'use strict';
  window.addEventListener('load', function () {
    // Prendi tutti i form a cui vogliamo applicare stili di validazione Bootstrap
    var forms = document.getElementsByClassName('needs-validation');
    // Impedisce l'invio del form se non è valido
    var validation = Array.prototype.filter.call(forms, function (form) {
      form.addEventListener('submit', function (event) {
        if (form.checkValidity() === false) {
          event.preventDefault();
          event.stopPropagation();
        }
        form.classList.add('was-validated');
      }, false);
    });
  }, false);
})();

/* Per selezionare tipo utente in fase di SignIn */
function selectCard(card) {
  var cards = document.querySelectorAll('.card-selectable');
  cards.forEach(function (c) {
      c.classList.remove('selected');
      c.querySelector('.card-radio').checked = false;
  });
  card.classList.add('selected');
  card.querySelector('.card-radio').checked = true;
}

/* Per generare pagina .cshtml tra le opzioni per l'utente loggato */
function redirectToPage(card) {
  const pageURL = card.getAttribute('data-url');
  window.location.href = pageURL; // Aggiunge l'estensione .html all'URL
}

$(document).ready(function () {
  // Controlla se c'è un messaggio di errore e mostra il modal
  var errorMessage = $('#errorMessageBody').text();
  if (errorMessage.trim().length > 0) {
      $('#errorModal').modal('show');
  }
});

function toggleFilter(filter) {
  var button = document.querySelector(`button[data-filter="${filter}"]`);
  var isActive = button.classList.contains('active');
  
  if (isActive) {
      button.classList.remove('active');
      button.style.backgroundColor = ''; // Ripristina il colore predefinito del pulsante
  } else {
      button.classList.add('active');
  }
  
  // Rimuovi il focus dal pulsante dopo aver eseguito l'azione
  button.blur();
}

function controllaPulsanti() {
  var dataInizio = document.getElementById('dataInizio').value;
  var dataFine = document.getElementById('dataFine').value;
  var oraInizio = document.getElementById('oraInizio').value;
  var oraFine = document.getElementById('oraFine').value;

  var sostaRicaricaAttivo = document.querySelector('button[data-filter="sosta"]').classList.contains('active') || document.querySelector('button[data-filter="ricarica"]').classList.contains('active');
  var utentiBasePremiumAttivo = document.querySelector('button[data-filter="utenti_base"]').classList.contains('active') || document.querySelector('button[data-filter="utenti_premium"]').classList.contains('active');
  
  var erroreSpan = document.getElementById('erroreSpan');
  
  if (dataInizio && dataFine && oraInizio && oraFine && sostaRicaricaAttivo && utentiBasePremiumAttivo) {
      // Se tutti i campi sono riempiti e almeno un pulsante tra "sosta" e "ricarica" è attivo e almeno uno tra "utenti_base" e "utenti_premium" è attivo, invia il modulo
      console.log("Modulo inviato correttamente.");
      document.querySelector('form').submit();
  } else {
      console.log("Modulo non inviato correttamente.");
      // Altrimenti, mostra un messaggio di errore
      erroreSpan.innerText = "Riempi tutti i campi e seleziona almeno un'opzione per i pagamenti e un'opzione per gli utenti.";
      erroreSpan.style.display = 'inline'; // Mostra lo span di errore
      // Puoi anche bloccare l'invio del modulo se preferisci
      // return false;
  }
}

$(function () {
  $('#datetimepicker1').datetimepicker({
      format: 'YYYY-MM-DD HH:mm' // Formato della data e dell'ora
  });

  $('#datetimepicker2').datetimepicker({
      format: 'YYYY-MM-DD HH:mm' // Formato della data e dell'ora
  });
});


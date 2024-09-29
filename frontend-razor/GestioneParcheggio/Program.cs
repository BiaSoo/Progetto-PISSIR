using System.Text;
using GestioneParcheggio.Services;
using MQTTnet;
using MQTTnet.Client;
using MQTTnet.Exceptions;
using MQTTnet.Protocol;
using Newtonsoft.Json;
using GestioneParcheggio.mqtt;

var builder = WebApplication.CreateBuilder(args);

// Add necessary services
builder.Services.AddRazorPages();
builder.Services.AddControllersWithViews(); // If you need controllers
builder.Services.AddHttpClient();
builder.Services.AddHttpContextAccessor();
builder.Services.AddDistributedMemoryCache();
builder.Services.AddSession();
builder.Services.AddSingleton<AuthService>();
builder.Services.AddSingleton<TokenValidator>();
builder.Services.AddSingleton<MqttService>();

var app = builder.Build();

if (app.Environment.IsDevelopment())
{
    app.UseDeveloperExceptionPage();
}
else
{
    app.UseExceptionHandler("/Error");
    app.UseHsts();
}

app.UseHttpsRedirection();
app.UseStaticFiles();
app.UseRouting();
app.UseSession();
app.UseAuthorization();
app.UseEndpoints(endpoints =>
{
    endpoints.MapRazorPages();
});

var factory = new MqttFactory();
var mqttClient = factory.CreateMqttClient();

// Create TCP based options using the builder.
var options = new MqttClientOptionsBuilder()
    .WithTcpServer("localhost", 1883) // Replace with your broker address
    .WithCleanSession()
    .Build();

// Handle message received
mqttClient.ApplicationMessageReceivedAsync += async e =>
{
    var topic = e.ApplicationMessage.Topic;
    var payload = e.ApplicationMessage.PayloadSegment.ToArray();
    var message = System.Text.Encoding.UTF8.GetString(payload);

    MqttService.ReceivedMessage = message;

    Console.WriteLine($"### RECEIVED APPLICATION MESSAGE ###");
    Console.WriteLine($"Topic: {topic}");
    Console.WriteLine($"Payload: {message}");

    // Aggiungi qui il codice per gestire il messaggio
    //GestisciRicaricaEffettuata(topic, message);


    await Task.CompletedTask;
};

// Connect to MQTT broker and subscribe to topic
try
{
    await mqttClient.ConnectAsync(options);
    Console.WriteLine("Connected to MQTT broker.");

    // Subscribe to a topic
    await mqttClient.SubscribeAsync(new MqttTopicFilterBuilder()
        .WithTopic("parcheggio/ricarica_effettuata")
        .WithQualityOfServiceLevel(MqttQualityOfServiceLevel.AtMostOnce)
        .Build());

    Console.WriteLine("Subscribed to topic.");
}
catch (MqttCommunicationException ex)
{
    Console.WriteLine($"MQTT Communication Exception: {ex.Message}");
}
catch (Exception ex)
{
    Console.WriteLine($"Exception: {ex.Message}");
}

app.MapRazorPages();
app.Run();

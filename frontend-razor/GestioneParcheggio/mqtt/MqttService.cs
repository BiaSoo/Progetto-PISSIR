using System;
using System.Threading;
using System.Threading.Tasks;
using MQTTnet;
using MQTTnet.Client;
using MQTTnet.Protocol;

namespace GestioneParcheggio.mqtt
{
    public class MqttService
    {
        private readonly IMqttClient _mqttClient;
        private readonly MqttClientOptions _mqttOptions;

        public static string? ReceivedMessage { get; set; }

        public MqttService()
        {
            var factory = new MqttFactory();
            _mqttClient = factory.CreateMqttClient();

            _mqttOptions = new MqttClientOptionsBuilder()
                .WithClientId("RazorPagesClient")
                .WithTcpServer("tcp://localhost:1883")
                .WithCleanSession()
                .Build();
        }

        public void Run()
        {
            while (true)
            {
                
            }
        }

        public async Task ConnectAsync()
        {
            _mqttClient.ConnectedAsync += async e =>
            {
                Console.WriteLine("Connected successfully with MQTT Brokers.");
                await Task.CompletedTask;
            };

            _mqttClient.DisconnectedAsync += async e =>
            {
                Console.WriteLine("Disconnected from MQTT Brokers.");
                await Task.CompletedTask;
            };

            await _mqttClient.ConnectAsync(_mqttOptions, CancellationToken.None);
        }

        public async Task PublishAsync(string topic, string payload)
        {
            var message = new MqttApplicationMessageBuilder()
                .WithTopic(topic)
                .WithPayload(payload)
                .WithQualityOfServiceLevel(MqttQualityOfServiceLevel.ExactlyOnce)
                .WithRetainFlag()
                .Build();

            if (_mqttClient.IsConnected)
            {
                await _mqttClient.PublishAsync(message, CancellationToken.None);
            }
        }
    }
}
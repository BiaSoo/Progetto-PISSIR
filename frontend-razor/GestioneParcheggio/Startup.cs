using GestioneParcheggio.Middleware;
using GestioneParcheggio.Pages;
using GestioneParcheggio.Services;
using GestioneParcheggio.mqtt;

namespace GestioneParcheggio
{
    public class Startup
    {
        public Startup(IConfiguration configuration)
        {
            Configuration = configuration;
        }

        public IConfiguration Configuration { get; }

        // This method gets called by the runtime. Use this method to add services to the container.
        public void ConfigureServices(IServiceCollection services)
        {
            services.AddRazorPages();
            services.AddHttpClient();
            services.AddSingleton<TokenValidator>();
            services.AddSingleton<AuthService>(); // Singleton instead of Transient
            services.AddTransient<IndexModel>();
            services.AddHttpContextAccessor();
            services.AddLogging();
            services.AddSingleton<MqttService>();
        }

        // This method gets called by the runtime. Use this method to configure the HTTP request pipeline.
        public void Configure(IApplicationBuilder app, IWebHostEnvironment env)
        {
            if (env.IsDevelopment())
            {
                app.UseDeveloperExceptionPage();
            }
            else
            {
                app.UseExceptionHandler("/Error");
                app.UseHsts();
            }

            app.UseMiddleware<TokenMiddleware>();
            app.UseHttpsRedirection();
            app.UseStaticFiles();

            app.UseRouting();

            app.UseAuthorization();

            app.UseEndpoints(endpoints =>
            {
                endpoints.MapRazorPages();
            });

            // Chiama ConnectAsync per stabilire la connessione MQTT
            var mqttService = app.ApplicationServices.GetService<MqttService>();
            mqttService?.ConnectAsync().Wait();
        }
    }
}

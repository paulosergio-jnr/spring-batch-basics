# spring-batch-basics

For many purposes, systems need to operate batch processes as scheduling tasks, importing, transforming or exporting data. In this article we will understand how batch 
processing works and how to use Spring Batch for performing it.

## Use case scenario
For this article we will consider a scenario where an application needs to import customer data from a *csv* file and generate another *csv* file that 
will be transmitted to an external system.

This input file contains customers from all over the world, but this application is responsible for exporting only the americans customers, 
ignoring foreign (non-american) customer data.

## Spring Batch
Spring Batch is a framework designed by Spring Team to enable an easier way to build robust and comprehensive batch applications.

A batch process usually is composed by tasks called jobs. Each _Job_ describes a processing flow or steps. Each _Step_ is composed of a reader, a processor and a writer, basically.

This schema is represented in the following draw:

![Spring Batch Schema](/documentation/images/SpringBatchScheme.png)

## Spring Initializr
Spring Initializr is a web tool with the purpose of generate Spring-based projects in an easy and quick way. We will make use of it in our project.

Head to https://start.spring.io/ and a website like the following will be shown:

![Spring Initializr](/documentation/images/Initializr.png)

For this project, we will define the group as *br.com.examples* and the artifact as *spring-batch-basics*. Now, we have to add some dependencies to our project in order to 
enable some features. Click on Add Dependencies and choose the following dependencies:

* Spring Batch
* H2 Database
* Lombok (optional)*

After the dependencies are set, click on Generate and a Zip file will be downloaded.

**Note**: Lombok is totally optional and we will use it in this project in order to make the code cleaner, specially on *Pojo* classes, 
simplifying *getters* and *setters* code. To learn more about Lombok, visit the website: https://projectlombok.org/

H2 database is needed in order to create Spring Batch process control tables. We will discuss this in a future advanced batch article.

## ItemReaders
In order to read data (input) to out batch process we need to use an *ItemReader*. *ItemReaders* are interfaces with just one method: *read()*. 
This interface is largely implemented by specific classes for specific objectives.

For this article we will use an implementation of ItemReader which objective is read from flat files where each line is a record. This implementation is a *FlatFileItemReader*. 

Any *ItemReader* implementation needs to be provided with a type. This type will define the resultant object class of the *read()* method.

The Type we need to inform here is equivalent to the record model, which will be named *CustomerInput*. 

```java
public class CustomerInput {

    private String id;

    private String firstName;

    private String lastName;

    private String gender;

    private String birthday;

    private String address;

    private String country;

}
```

**Note**: do not forget to implement *Getters* and *Setter* methods. As we are using Lombok in this example, the annotations **@Getter** and **@Setter** will do the job.

Our reader will transform each line of the file in a *CustomerInput* object which will be permeated to the ItemReader. Every time *read()* method is called, 
it will extract exactly one object from the file, although we can define a chunk size, which tells the batch handler to run *read()* method **n** times, working always with a batch of this size.

Now, we need to create our ItemReader class and for that, we will create a class named *CustomerItemReader*. This class will extend *FlatFileItemReader*, as the following example:

```java
public class CustomerItemReader extends FlatFileItemReader<CustomerInput> {

}
```

Next step is to configure our reader, setting the way it will transform a read line into an object of type *CustomerInput*. For this, we will override the constructor class, implementing this:

```java
// declared constants to better organize the code
private static final String DEFAULT_DELIMITER = ";";

private static final String ID_HEADER = "Id";
private static final String FIRST_NAME_HEADER = "first name";
private static final String LAST_NAME_HEADER = "last name";
private static final String GENDER_HEADER = "gender";
private static final String BIRTHDAY_HEADER = "birthday";
private static final String ADDRESS_HEADER = "address";
private static final String COUNTRY_HEADER = "country";

private static final int LINES_TO_SKIP = 1;

public CustomerItemReader(final ApplicationProperties properties) {
  // calls the superclass constructor
  super();

  // creates the workers that will separate delimited records into objects
  final DefaultLineMapper<CustomerInput> lineMapper = new DefaultLineMapper<>();
  final DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();

  // set columns names and delimiters
  tokenizer.setNames(ID_HEADER, FIRST_NAME_HEADER, LAST_NAME_HEADER, GENDER_HEADER, BIRTHDAY_HEADER, ADDRESS_HEADER, COUNTRY_HEADER);
  tokenizer.setDelimiter(DEFAULT_DELIMITER);

  lineMapper.setLineTokenizer(tokenizer);
  lineMapper.setFieldSetMapper(new FieldSetMapper<CustomerInput>() {
      @Override
      public CustomerInput mapFieldSet(final FieldSet fieldSet) throws BindException {
          // creates a new object and fill it with mapped values from file record
          CustomerInput input = new CustomerInput();

          input.setId(fieldSet.readString(ID_HEADER));
          input.setFirstName(fieldSet.readString(FIRST_NAME_HEADER));
          input.setLastName(fieldSet.readString(LAST_NAME_HEADER));
          input.setGender(fieldSet.readString(GENDER_HEADER));
          input.setBirthday(fieldSet.readString(BIRTHDAY_HEADER));
          input.setAddress(fieldSet.readString(ADDRESS_HEADER));
          input.setCountry(fieldSet.readString(COUNTRY_HEADER));

          return input;
      }
  });

  // define the resource (input file)
  this.setResource(new ClassPathResource(properties.getInputFile()));
  // define the line mapper
  this.setLineMapper(lineMapper);
  // as the first line of our file is a header, we set the reader to skip it
  this.setLinesToSkip(LINES_TO_SKIP);
}
```

We also need to create a bean of this class, annotating it with **@Service**:

```java
@Service
public class CustomerItemReader extends FlatFileItemReader<CustomerInput> {
```

Note that we are making use of a class named *ApplicationProperties*. This class is responsible for getting the parameters we define in *application.properties* and inject them into code:

```java
@Configuration
public class ApplicationProperties {

  @Value("${batch.input.file-name}")
  private String inputFile;

  @Value("${batch.output.americans.file-name}")
  private String americansFile;

}
```

This way we can change some parameters without having to deploy the application again. This is an example of the *applications.properties* file to parametrize our batch application:

```properties
// input file path relative to resources package
batch.input.file-name=/data/input/customers.csv

// output file path in file system
batch.output.americans.file-name=/tmp/americans.csv
```

## ItemProcessors
Now that we set up our reader, the next step is to implement our processor class. Processors are classes which purpose is to filter, enrich or apply business logic to the domain. 
If your application makes use of a *domain driven pattern*, it is highly recommended that your first processor in this step converts the input model to the domain 
model (we can have more than one step. Check the future articles to learn more).

For this application we will work with just one domain: *Customer* and every *Customer* has its own *Address*:

```java
@Getter
@Setter
public class Customer {

  private UUID id;

  private String firstName;

  private String lastName;

  private Gender gender;

  private LocalDate birthday;

  private Address address;

  private boolean foreign;

}
```

```java
@Getter
@Setter
public class Address {

  private String street;

  private String country;

}
```

We also need to create an Enum class in order to define properly the customer gender. This file will be named Gender as the following:

```java
public enum Gender {

  MALE("male"),
  FEMALE("female");

  private String value;

  Gender(final String gender) {
    this.value = gender;
  }

  public String getValue() {
    return value;
  }

  public static Gender entryOf(final String gender) {
    for (Gender g : Gender.values()) {
      if (g.getValue().equals(gender)) {
        return g;
      }
    }
    return null;
  }
}
```

As soon as our _ItemProcessor_ is called, we will map _CustomerInput_ into the domain _Customer_, assigning the right types as 
_LocalDate_ and _Gender_. As mentioned before, one of _ItemReader_ functionalities is to filter objects and that is one of the things 
we will implement in our _ItemReader_. Every _Customer_ whose country is different from United States will be discarded and not flowing to the _ItemWriter_.

For that, we create a class _CustomerItemProcessor_ with these implementations:

```java
@Service
public class CustomerItemProcessor implements ItemProcessor<CustomerInput, Customer> {

  private static final String UNITED_STATES_COUNTRY = "United States";
  private static final String DATE_PATTERN = "yyyy-MM-dd";

  @Override
  public Customer process(final CustomerInput customerInput) throws Exception {
    // filtering american customers. Any foreign customer will be discarded
    if (! UNITED_STATES_COUNTRY.equals(customerInput.getCountry())) {
      return null;
    }

    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
    final Customer customer = new Customer();
    final Address address = new Address();

    customer.setId(UUID.fromString(customerInput.getId()));
    customer.setFirstName(customerInput.getFirstName());
    customer.setLastName(customerInput.getLastName());
    customer.setGender(Gender.entryOf(customerInput.getGender()));
    customer.setBirthday(LocalDate.parse(customerInput.getBirthday(), formatter));

    address.setStreet(customerInput.getAddress());
    address.setCountry(customerInput.getCountry());

    customer.setAddress(address);

    return customer;
  }
}
```

Things to have in mind here:

* This _ItemProcessor_ is needed to be a _Bean_, therefore we use the **@Service** annotation;
* _ItemProcessor_ needs to be given two types, the Input and Output classes. In this example, _CustomerInput_ and _Customer_, respectively;
* To discard an object on _ItemReader_ (filtering functionality), you just need to return null on the _process()_ method.

## ItemWriters
The final operation in a batch process is the output, or the writing, and for this we use an _ItemWriter_. As we will write a new file, we use then a _FlatFileItemWriter_.

For our _ItemWriter_, we will name it _CustomerItemWriter_ and make it extend from _FlatFileItemWriter_ class. We need to configure this class in order to make it write our file properly as the following code:

```java
@Service
public class CustomerItemWriter extends FlatFileItemWriter<Customer> {

  // declared constants to better organize the code
  private static final String DEFAULT_DELIMITER = ";";
  private static final String DEFAULT_LINE_BREAK = "\n";

  private static final String ID_HEADER = "Id";
  private static final String FIRST_NAME_HEADER = "first_name";
  private static final String LAST_NAME_HEADER = "last_name";
  private static final String GENDER_HEADER = "gender";
  private static final String BIRTHDAY_HEADER = "birthday";
  private static final String ADDRESS_HEADER = "address";

  // dependency injection of properties class
  @Autowired
  ApplicationProperties properties;

  public CustomerItemWriter(final ApplicationProperties properties) {
    // calls superclass constructor in order to basically configure the object
    super();

    // defining of line aggregator and fieldExtractor
    final DelimitedLineAggregator<Customer> lineAggregator = new DelimitedLineAggregator<Customer>();
    final BeanWrapperFieldExtractor<Customer> fieldExtractor = new BeanWrapperFieldExtractor<Customer>();

    lineAggregator.setDelimiter(DEFAULT_DELIMITER);
    lineAggregator.setFieldExtractor(fieldExtractor);

    // setting output file and default line aggregator
    this.setResource(new FileSystemResource(properties.getAmericansFile()));
    this.setLineAggregator(lineAggregator);
  }

  @Override
  public String doWrite(final List<? extends Customer> items) {
    // creates a StringBuilder which is receive the lines to write
    final StringBuilder linesToWrite = new StringBuilder();

    items.forEach(customer -> {
      // creates line with Customer properties
      final String line = new StringBuilder()
          .append(customer.getId())
          .append(DEFAULT_DELIMITER)
          .append(customer.getFirstName())
          .append(DEFAULT_DELIMITER)
          .append(customer.getLastName())
          .append(DEFAULT_DELIMITER)
          .append(customer.getBirthday())
          .append(DEFAULT_DELIMITER)
          .append(customer.getGender().getValue())
          .append(DEFAULT_DELIMITER)
          .append(customer.getAddress().getStreet())
          .append(DEFAULT_DELIMITER)
          .append(customer.getAddress().getCountry())
          .toString();

      // appends line to StringBuilder
      linesToWrite.append(line).append(DEFAULT_LINE_BREAK);
    });

    // returns lines to write (limited to chunk size)
    return linesToWrite.toString();
  }

}
```

An _ItemWriter_ receives a _Type_, to indicate what kind of class it will use in order to write the content. As we are working with the _Customer_ domain, 
we will use it as _Type_. _ItemWriters_ work differently from _ItemReaders_ and _ItemProcessors_ in terms of processing content. Instead of working with single objects, 
an _ItemWriter_ receives a list of objects in _write()_ method. This list has, at maximum, the chunk defined size in order to make the writing process more efficient.

## Jobs and Steps
As said before, _Jobs_ are composed of _Steps_, that will achieve the expected tasks. In our scenario we will only need one job and one step due to the simplicity of our task.

_Jobs_ are configurations that will describe the step flow and _Steps_ are configurations which will describe how a task should work, defining the operation size, input, processing and output.

That said, we will create a configuration class that will provide our _Job_ and _Step_. Let us start creating a class named _CustomerBatchConfiguration_. As its purpose, 
we need to create a bean of _Configuration_ out of this class annotating it with **@Configuration**. We also need to set the **@EnableBatchProcessing** annotation in this 
class in order to let Spring define configurations and contexts.

After creating and annotating our Configuration class, inject the needed dependencies as the following:

```java
  @Autowired
  private JobBuilderFactory jobBuilderFactory;

  @Autowired
  private StepBuilderFactory stepBuilderFactory;

  @Autowired
  private JobLauncher jobLauncher;

  @Autowired
  ApplicationProperties applicationProperties;

  @Autowired
  private CustomerItemReader customerItemReader;

  @Autowired
  private CustomerItemProcessor customerItemProcessor;

  @Autowired
  private CustomerItemWriter customerItemWriter;
```

After that, we define our _Step_ properly:

```java
@Bean
public Step importCustomersStep() {
  return stepBuilderFactory.get("STEP-CUSTOMERS-01")
      // defines step input and output domains and chunk size
      .<CustomerInput, Customer>chunk(10)
      // defines the reader
      .reader(customerItemReader)
      // defines the processor
      .processor(customerItemProcessor)
      // defines the writer
      .writer(customerItemWriter)
      .build();
}
```

With our Step configured, we need to define our _Job_:

```java
@Bean
public Job importCustomersJob() {
  return jobBuilderFactory.get("JOB-IMPORT-CUSTOMER")
      // defines the step flow. You can add more steps in a Job by using next() method
      .flow(importCustomersStep())
      // end configuration
      .end()
      .build();
}
```

## Running it up
That should be enough coding in order to run the application. The next thing to do is to create our input file inside the expected resource package. 
It should look like _<project>/src/main/resources/data/input/customers.csv_. 

An example input file is provided by following this [link](https://github.com/pspjnsu/spring-batch-basics/blob/master/src/main/resources/data/input/customers.csv). 
You can download the file content and place it in the right directory.

Now, let us compile the project running the following command in Maven:

`mvn clean compile`

If everything goes right, you will receive a message like the following indicating the compilation process was successful:

```
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  3.977 s
[INFO] Finished at: 2020-06-14T15:35:14-03:00
[INFO] ------------------------------------------------------------------------
```

After the compilation, we can just run the application, again using Maven:

`mvn spring-boot:run`

Running it successfully will display a message like:

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v2.3.0.RELEASE)

2020-06-14 15:39:07.158  INFO 7682 --- [           main] b.c.e.s.SpringBatchBasicsApplication     : Starting SpringBatchBasicsApplication on user-Q501LA with PID 7682 (/home/user/workspace/spring-batch-basics/target/classes started by paulo in /home/user/workspace/spring-batch-basics)
2020-06-14 15:39:07.161  INFO 7682 --- [           main] b.c.e.s.SpringBatchBasicsApplication     : No active profile set, falling back to default profiles: default
2020-06-14 15:39:07.926  INFO 7682 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2020-06-14 15:39:08.010  INFO 7682 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2020-06-14 15:39:08.155  INFO 7682 --- [           main] o.s.b.c.r.s.JobRepositoryFactoryBean     : No database type set, using meta data indicating: H2
2020-06-14 15:39:08.218  INFO 7682 --- [           main] o.s.b.c.l.support.SimpleJobLauncher      : No TaskExecutor has been set, defaulting to synchronous executor.
2020-06-14 15:39:08.301  INFO 7682 --- [           main] b.c.e.s.SpringBatchBasicsApplication     : Started SpringBatchBasicsApplication in 1.445 seconds (JVM running for 1.771)
2020-06-14 15:39:08.302  INFO 7682 --- [           main] o.s.b.a.b.JobLauncherApplicationRunner   : Running default command line with: []
2020-06-14 15:39:08.348  INFO 7682 --- [           main] o.s.b.c.l.support.SimpleJobLauncher      : Job: [FlowJob: [name=JOB-IMPORT-CUSTOMER]] launched with the following parameters: [{}]
2020-06-14 15:39:08.385  INFO 7682 --- [           main] o.s.batch.core.job.SimpleStepHandler     : Executing step: [STEP-CUSTOMERS-01]
2020-06-14 15:39:08.442  INFO 7682 --- [           main] o.s.batch.core.step.AbstractStep         : Step: [STEP-CUSTOMERS-01] executed in 57ms
2020-06-14 15:39:08.450  INFO 7682 --- [           main] o.s.b.c.l.support.SimpleJobLauncher      : Job: [FlowJob: [name=JOB-IMPORT-CUSTOMER]] completed with the following parameters: [{}] and the following status: [COMPLETED] in 81ms
2020-06-14 15:39:08.452  INFO 7682 --- [extShutdownHook] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Shutdown initiated...
2020-06-14 15:39:08.455  INFO 7682 --- [extShutdownHook] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Shutdown completed.
```

**Optionally** you can use the features provided by your IDE to compile and run the application easily.

Notice that the application output will display important information about our Job:

* Job starting time and parameters;
* Step starting time;
* Step return status and duration;
* Job return status and duration.

These information are essential for applications, specially when we have to do any kind of troubleshooting.

After this execution, checkout the output file set by `batch.output.americans.file-name parameter` in `application.properties` file. If the process was successful, 
this file will contain just the american customers in the resultant .csv file and with the headers that we previously set.

In order to monitor all steps while being executed, you can implement loggers in every phase of the process: readers, processors and writers.

## Conclusion
Batch processes are still present in modern system architectures like in importing, enriching and purging jobs that are executed periodically. 
Spring Batch helps us modelling and developing batch processesses from scratch in a easier and robust way.

In the next articles we will build reliable applications, understanding more features like classification and composition, different types of _ItemReaders_ and _ItemWriters_, and parallel processing.

For the complete example, visit the official repository of this article on: https://github.com/pspjnsu/spring-batch-basics.

You can also find more information about Spring Batch, _ItemReaders_, _ItemProcessors_ and _ItemWriters_ in the official documentation: https://docs.spring.io/spring-batch/docs/4.2.x/reference/html/index.html

Thanks and see you in the next articles.


## References

https://docs.spring.io/spring-batch/docs/4.2.x/reference/html/index.html

https://docs.spring.io/spring-batch/docs/4.2.x/api/index.html

https://docs.spring.io/spring-batch/docs/current/reference/html/readersAndWriters.html

https://docs.spring.io/spring-batch/docs/current/api/org/springframework/batch/item/ItemReader.html
